package com.edu.unimag.rodatours.repositories;

import com.edu.unimag.rodatours.entities.Tour;
import com.edu.unimag.rodatours.entities.enums.DifficultyLevel;
import com.edu.unimag.rodatours.entities.enums.TourStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Consultas del catálogo de tours y de disponibilidad por destino y fecha.
 */
public interface TourRepository extends JpaRepository<Tour, Long> {

    List<Tour> findByAgenciaId(Long agenciaId);

    List<Tour> findByAgenciaIdAndEstado(Long agenciaId, TourStatus estado);

    List<Tour> findByAgenciaIdAndNivelDificultad(Long agenciaId, DifficultyLevel nivelDificultad);

    List<Tour> findByAgenciaIdAndDestinoContainingIgnoreCase(Long agenciaId, String destino);

    List<Tour> findByEstadoAndDestinoContainingIgnoreCase(TourStatus estado, String destino);

    /**
     * Tours activos con al menos una salida, en la fecha indicada, con cupos reales.
     * Se usa {@code distinct} porque un tour puede tener varias salidas el mismo día.
     */
    @Query("""
            select distinct t from Tour t
            join t.horarios h
            where t.estado = :estado
              and (:destino is null or lower(t.destino) like lower(concat('%', :destino, '%')))
              and h.fechaTour = :fecha
              and h.cuposDisponibles >= :cantidadPersonas
            order by t.titulo asc
            """)
    List<Tour> findAvailableByDestinationAndDate(@Param("estado") TourStatus estado,
                                                 @Param("destino") String destino,
                                                 @Param("fecha") LocalDate fecha,
                                                 @Param("cantidadPersonas") int cantidadPersonas);
}
