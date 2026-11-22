package com.edu.unimag.rodatours.domain.repositories;

import com.edu.unimag.rodatours.domain.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Consultas de las reseñas ligadas a reservas completadas.
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByReservaId(Long reservaId);

    boolean existsByReservaId(Long reservaId);

    List<Review> findByReservaTuristaId(Long turistaId);
}
