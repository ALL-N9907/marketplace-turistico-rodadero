package com.edu.unimag.rodatours.repositories;

import com.edu.unimag.rodatours.entities.TourSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Agenda de salidas, control de cupos y bloqueo pesimista para confirmaciones.
 */
public interface TourScheduleRepository extends JpaRepository<TourSchedule, Long> {

    List<TourSchedule> findByTourIdOrderByFechaTourAscHoraInicioAsc(Long tourId);

    List<TourSchedule> findByTourIdAndFechaTourOrderByHoraInicioAsc(Long tourId, LocalDate fechaTour);

    boolean existsByTourIdAndFechaTourAndHoraInicio(Long tourId, LocalDate fechaTour, LocalTime horaInicio);

    List<TourSchedule> findByGuiaIdAndFechaTourInOrderByHoraInicioAsc(Long guiaId, Collection<LocalDate> fechas);

    @Query("""
            select s from TourSchedule s
            where s.tour.id = :tourId
              and s.fechaTour = :fecha
              and s.cuposDisponibles >= :cantidadPersonas
            order by s.horaInicio asc
            """)
    List<TourSchedule> findWithAvailableSpots(@Param("tourId") Long tourId,
                                              @Param("fecha") LocalDate fecha,
                                              @Param("cantidadPersonas") int cantidadPersonas);

    /**
     * Bloquea la fila de la salida mientras se descuentan los cupos al confirmar una reserva.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TourSchedule s where s.id = :id")
    Optional<TourSchedule> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select coalesce(sum(s.cuposTotales), 0) from TourSchedule s
            where s.tour.agencia.id = :agenciaId
              and s.fechaTour between :desde and :hasta
            """)
    Long sumTotalSpotsByAgencyAndDateRange(@Param("agenciaId") Long agenciaId,
                                           @Param("desde") LocalDate desde,
                                           @Param("hasta") LocalDate hasta);

    /**
     * El rango de una salida se calcula con la duración del tour, que no está persistida
     * en la tabla de horarios; por eso se filtra por día y se cierra con las reglas de la entidad.
     */
    default boolean existsOverlappingScheduleForGuide(Long guiaId, LocalDateTime inicio, LocalDateTime fin) {
        if (guiaId == null || inicio == null || fin == null || !inicio.isBefore(fin)) {
            return false;
        }

        return findByGuiaIdAndFechaTourInOrderByHoraInicioAsc(
                guiaId, List.of(inicio.toLocalDate(), fin.toLocalDate())).stream()
                .anyMatch(schedule -> {
                    LocalDateTime scheduleStart = schedule.getStartDateTime();
                    LocalDateTime scheduleEnd = schedule.calculateEndDateTime();
                    return scheduleStart != null && scheduleEnd != null
                            && scheduleStart.isBefore(fin)
                            && scheduleEnd.isAfter(inicio);
                });
    }
}
