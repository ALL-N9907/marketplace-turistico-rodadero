package com.edu.unimag.rodatours.domain.repositories;

import com.edu.unimag.rodatours.domain.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Consultas de las reseñas ligadas a reservas completadas.
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByReservaId(Long reservaId);

    boolean existsByReservaId(Long reservaId);

    List<Review> findByReservaTuristaId(Long turistaId);

    Page<Review> findByReservaHorarioTourIdAndReservaEstado(Long tourId, ReservationStatus estado, Pageable pageable);
    Page<Review> findByReservaHorarioTourIdAndReservaEstadoAndPublishedTrue(Long tourId, ReservationStatus estado, Pageable pageable);

    @Query("select avg(r.calificacion) from Review r where r.reserva.horario.tour.id = :tourId and r.reserva.estado = :state and r.published = true")
    Double averageByTour(@Param("tourId") Long tourId, @Param("state") ReservationStatus state);
}
