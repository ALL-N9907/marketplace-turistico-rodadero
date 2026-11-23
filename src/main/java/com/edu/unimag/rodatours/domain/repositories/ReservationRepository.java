package com.edu.unimag.rodatours.domain.repositories;

import com.edu.unimag.rodatours.domain.entities.Reservation;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.projections.AgencyOccupancyProjection;
import com.edu.unimag.rodatours.domain.repositories.projections.AgencyProductivityProjection;
import com.edu.unimag.rodatours.domain.repositories.projections.ExperienceTypeCancellationProjection;
import com.edu.unimag.rodatours.domain.repositories.projections.NoShowTouristProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Reservas y consultas de validación y reportes descritas en el documento del proyecto.
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

    Optional<Reservation> findByCodigoQr(String codigoQr);

    List<Reservation> findByTuristaIdAndEstado(Long turistaId, ReservationStatus estado);

    List<Reservation> findByTuristaIdOrderByFechaReservaDesc(Long turistaId);

    List<Reservation> findByHorarioId(Long horarioId);

    boolean existsByTuristaId(Long turistaId);

    boolean existsByHorarioTourId(Long tourId);

    boolean existsByHorarioIdAndEstadoIn(Long horarioId, Collection<ReservationStatus> estados);

    boolean existsByHorarioTourIdAndEstadoIn(Long tourId, Collection<ReservationStatus> estados);

    /**
     * Personas retenidas por reservas en estado dado para una salida. Con SCHEDULED
     * se obtienen los cupos "retenidos" por reservas pendientes de confirmación.
     */
    @Query("""
            select coalesce(sum(r.cantidadPersonas), 0) from Reservation r
            where r.horario.id = :horarioId
              and r.estado = :estado
            """)
    Long sumPersonasByHorarioIdAndEstado(@Param("horarioId") Long horarioId,
                                         @Param("estado") ReservationStatus estado);

    List<Reservation> findByFechaReservaBetween(LocalDateTime desde, LocalDateTime hasta);

    List<Reservation> findByFechaInicioBetween(LocalDateTime desde, LocalDateTime hasta);

    /**
     * Traslape de reservas activas: el nuevo rango [inicio, fin) no debe cruzar
     * ningún rango existente.
     */
    @Query("""
            select count(r) from Reservation r
            where r.turista.id = :turistaId
              and r.estado in :estados
              and r.fechaInicio < :fin
              and r.fechaFin > :inicio
            """)
    long countOverlappingByTuristaAndStates(@Param("turistaId") Long turistaId,
                                            @Param("estados") Collection<ReservationStatus> estados,
                                            @Param("inicio") LocalDateTime inicio,
                                            @Param("fin") LocalDateTime fin);

    default boolean existsOverlappingByTuristaAndStates(Long turistaId,
                                                        Collection<ReservationStatus> estados,
                                                        LocalDateTime inicio,
                                                        LocalDateTime fin) {
        return countOverlappingByTuristaAndStates(turistaId, estados, inicio, fin) > 0;
    }

    /**
     * Cupos tomados por reservas confirmadas o completadas de una agencia en el rango.
     */
    @Query("""
            select coalesce(sum(r.cantidadPersonas), 0) from Reservation r
            where r.horario.tour.agencia.id = :agenciaId
              and r.horario.fechaTour between :desde and :hasta
              and r.estado in :estados
            """)
    Long sumOccupiedSpotsByAgencyAndDateRange(@Param("agenciaId") Long agenciaId,
                                              @Param("desde") LocalDate desde,
                                              @Param("hasta") LocalDate hasta,
                                              @Param("estados") Collection<ReservationStatus> estados);

    @Query("""
            select a.id as agenciaId,
                   a.nombreComercial as nombreComercial,
                   (select coalesce(sum(s.cuposTotales), 0) from TourSchedule s
                    where s.tour.agencia = a
                      and s.fechaTour between :desde and :hasta) as cuposTotales,
                   (select coalesce(sum(r.cantidadPersonas), 0) from Reservation r
                    where r.horario.tour.agencia = a
                      and r.horario.fechaTour between :desde and :hasta
                      and r.estado in :estados) as cuposOcupados
            from Agency a
            where a.id = :agenciaId
            """)
    Optional<AgencyOccupancyProjection> findAgencyOccupancy(@Param("agenciaId") Long agenciaId,
                                                            @Param("desde") LocalDate desde,
                                                            @Param("hasta") LocalDate hasta,
                                                            @Param("estados") Collection<ReservationStatus> estados);

    @Query("""
            select r.horario.tour.tipoExperiencia.nombre as tipoExperiencia,
                   r.estado as estado,
                   count(r) as cantidad
            from Reservation r
            where r.estado in :estados
              and r.horario.fechaTour between :desde and :hasta
            group by r.horario.tour.tipoExperiencia.nombre, r.estado
            order by r.horario.tour.tipoExperiencia.nombre asc, r.estado asc
            """)
    List<ExperienceTypeCancellationProjection> countByExperienceTypeAndState(
            @Param("estados") Collection<ReservationStatus> estados,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query("""
            select r.horario.tour.agencia.id as agenciaId,
                   r.horario.tour.agencia.nombreComercial as nombreComercial,
                   count(r) as toursCompletados
            from Reservation r
            where r.estado = :estado
              and r.horario.fechaTour between :desde and :hasta
            group by r.horario.tour.agencia.id, r.horario.tour.agencia.nombreComercial
            order by count(r) desc, r.horario.tour.agencia.nombreComercial asc
            """)
    List<AgencyProductivityProjection> rankingAgenciesByCompletedTours(
            @Param("estado") ReservationStatus estado,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query("""
            select r.turista.id as turistaId,
                   r.turista.nombreCompleto as nombreCompleto,
                   count(r) as cantidadNoShow
            from Reservation r
            where r.estado = :estado
              and r.horario.fechaTour between :desde and :hasta
            group by r.turista.id, r.turista.nombreCompleto
            order by count(r) desc, r.turista.nombreCompleto asc
            """)
    List<NoShowTouristProjection> findNoShowTourists(@Param("estado") ReservationStatus estado,
                                                     @Param("desde") LocalDate desde,
                                                     @Param("hasta") LocalDate hasta);
}
