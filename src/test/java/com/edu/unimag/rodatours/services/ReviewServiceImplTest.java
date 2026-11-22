package com.edu.unimag.rodatours.services;

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
import com.edu.unimag.rodatours.services.exceptions.BusinessException;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.impl.ReviewServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TouristRepository touristRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private final Tourist owner = Tourist.builder().id(1L).build();

    @Test
    @DisplayName("create() crea reseña solo cuando la reserva está COMPLETED y no tiene reseña previa")
    void create_success() {
        Reservation completed = Reservation.builder().id(10L).turista(owner).estado(ReservationStatus.COMPLETED).build();
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(completed));
        when(reviewRepository.existsByReservaId(10L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(5L);
            return r;
        });

        ReviewResponse response = reviewService.create(1L, new CreateReviewRequest(10L, 5, "Excelente guía"));

        assertEquals(5L, response.id());
        assertEquals(10L, response.reservaId());
        assertEquals(5, response.calificacion());
    }

    @Test
    @DisplayName("create() lanza BusinessException si la reserva no está en estado COMPLETED")
    void create_notCompletedReservation_throwsBusinessException() {
        Reservation confirmed = Reservation.builder().id(10L).turista(owner).estado(ReservationStatus.CONFIRMED).build();
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(confirmed));

        assertThrows(BusinessException.class,
                () -> reviewService.create(1L, new CreateReviewRequest(10L, 4, "Muy bueno")));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("create() lanza ConflictException si la reserva ya tiene una reseña")
    void create_duplicateReview_throwsConflictException() {
        Reservation completed = Reservation.builder().id(10L).turista(owner).estado(ReservationStatus.COMPLETED).build();
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(completed));
        when(reviewRepository.existsByReservaId(10L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> reviewService.create(1L, new CreateReviewRequest(10L, 5, "Repetida")));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("getByTouristId() valida existencia del turista y devuelve sus reseñas")
    void getByTouristId_successAndNotFound() {
        when(touristRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> reviewService.getByTouristId(99L));

        Reservation res = Reservation.builder().id(10L).turista(owner).build();
        Review review = Review.builder().id(5L).reserva(res).calificacion(5).comentario("Genial").build();
        when(touristRepository.existsById(1L)).thenReturn(true);
        when(reviewRepository.findByReservaTuristaId(1L)).thenReturn(List.of(review));

        List<ReviewResponse> list = reviewService.getByTouristId(1L);
        assertEquals(1, list.size());
        assertEquals(5, list.get(0).calificacion());
    }

    @Test
    @DisplayName("update() y delete() actualizan y eliminan reseñas existentes")
    void updateAndDelete_success() {
        Reservation res = Reservation.builder().id(10L).turista(owner).build();
        Review review = Review.builder().id(5L).reserva(res).calificacion(3).comentario("Regular").build();

        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse updated = reviewService.update(5L, 1L, new UpdateReviewRequest(5, "Mejorado"));
        assertEquals(5, updated.calificacion());
        assertEquals("Mejorado", updated.comentario());

        reviewService.delete(5L, 1L);
        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("create(), update() y delete() lanzan BusinessException si el turista no es el dueño de la reserva")
    void ownership_violations_throwBusinessException() {
        Reservation completed = Reservation.builder().id(10L).turista(owner).estado(ReservationStatus.COMPLETED).build();
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(completed));

        assertThrows(BusinessException.class,
                () -> reviewService.create(2L, new CreateReviewRequest(10L, 5, "Ajena")));
        assertThrows(BusinessException.class,
                () -> reviewService.create(null, new CreateReviewRequest(10L, 5, "Sin turista")));

        Review review = Review.builder().id(5L).reserva(completed).calificacion(3).comentario("Regular").build();
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        assertThrows(BusinessException.class,
                () -> reviewService.update(5L, 2L, new UpdateReviewRequest(5, "Cambio ajeno")));
        assertThrows(BusinessException.class, () -> reviewService.delete(5L, 2L));

        verify(reviewRepository, never()).save(any());
        verify(reviewRepository, never()).delete(any());
    }
}
