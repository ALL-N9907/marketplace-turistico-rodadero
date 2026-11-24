package com.edu.unimag.rodatours.services.impl;

import com.edu.unimag.rodatours.domain.entities.Tourist;
import com.edu.unimag.rodatours.api.dtos.request.CreateReviewRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateReviewRequest;
import com.edu.unimag.rodatours.api.dtos.response.ReviewResponse;
import com.edu.unimag.rodatours.domain.entities.Reservation;
import com.edu.unimag.rodatours.domain.entities.Review;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.domain.repositories.ReviewRepository;
import com.edu.unimag.rodatours.domain.repositories.TouristRepository;
import com.edu.unimag.rodatours.services.ReviewService;
import com.edu.unimag.rodatours.services.exceptions.BusinessException;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final TouristRepository touristRepository;

    @Override
    public ReviewResponse create(Long turistaId, CreateReviewRequest request) {
        Reservation reservation = reservationRepository.findById(request.reservaId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con id: " + request.reservaId()));

        assertOwnedBy(reservation, turistaId);

        if (reservation.getEstado() != ReservationStatus.COMPLETED) {
            throw new BusinessException("Solo se puede reseñar una reserva en estado COMPLETED.");
        }
        if (reviewRepository.existsByReservaId(reservation.getId())) {
            throw new ConflictException("La reserva ya cuenta con una reseña registrada.");
        }

        Review review = ReviewMapper.toEntity(request, reservation);
        Review saved = reviewRepository.save(review);
        return ReviewMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getById(Long id) {
        Review review = findReviewOrThrow(id);
        if (!review.isPublished()) throw new ResourceNotFoundException("Reseña no disponible.");
        return ReviewMapper.toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getByReservationId(Long reservaId) {
        Review review = reviewRepository.findByReservaId(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe reseña para la reserva con id: " + reservaId));
        return ReviewMapper.toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getByTouristId(Long turistaId) {
        if (!touristRepository.existsById(turistaId)) {
            throw new ResourceNotFoundException("Turista no encontrado con id: " + turistaId);
        }
        return reviewRepository.findByReservaTuristaId(turistaId).stream()
                .map(ReviewMapper::toResponse)
                .toList();
    }

    @Override
    public ReviewResponse update(Long id, Long turistaId, UpdateReviewRequest request) {
        Review review = findReviewOrThrow(id);
        assertOwnedBy(review.getReserva(), turistaId);
        ReviewMapper.applyUpdate(request, review);
        return ReviewMapper.toResponse(reviewRepository.save(review));
    }

    @Override
    public void delete(Long id, Long turistaId) {
        Review review = findReviewOrThrow(id);
        assertOwnedBy(review.getReserva(), turistaId);
        reviewRepository.delete(review);
    }

    /** La reseña solo puede gestionarla el turista dueño de la reserva (en la API, el usuario autenticado). */
    private static void assertOwnedBy(Reservation reservation, Long turistaId) {
        Tourist owner = reservation.getTurista();
        if (turistaId == null || owner == null || !turistaId.equals(owner.getId())) {
            throw new BusinessException("La reserva no pertenece al turista indicado.");
        }
    }

    private Review findReviewOrThrow(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada con id: " + id));
    }
}
