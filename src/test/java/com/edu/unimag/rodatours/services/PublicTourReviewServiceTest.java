package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.domain.entities.*;
import com.edu.unimag.rodatours.domain.entities.enums.*;
import com.edu.unimag.rodatours.domain.repositories.ReviewRepository;
import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicTourReviewServiceTest {
    @Mock TourRepository tours;
    @Mock ReviewRepository reviews;
    @InjectMocks PublicTourReviewService service;

    @Test
    void returnsStablePageAndAverageOfCompletedReservationsOnly() {
        when(tours.findById(1L)).thenReturn(Optional.of(Tour.builder().agencia(Agency.builder().build()).build()));
        var now = LocalDateTime.now();
        var review = Review.builder().id(8L).calificacion(5).comentario("Excelente").fechaCreacion(now).build();
        when(reviews.findByReservaHorarioTourIdAndReservaEstadoAndPublishedTrue(eq(1L), eq(ReservationStatus.COMPLETED), any()))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(1, 5), 6));
        when(reviews.averageByTour(1L, ReservationStatus.COMPLETED)).thenReturn(4.5);
        var result = service.list(1L, 1, 5);
        assertEquals(6, result.totalElements());
        assertEquals(2, result.totalPages());
        assertEquals(4.5, result.averageRating());
        assertEquals(new PublicTourReviewService.PublicReview(8L, 5, "Excelente", now), result.content().getFirst());
        verify(reviews).findByReservaHorarioTourIdAndReservaEstadoAndPublishedTrue(1L, ReservationStatus.COMPLETED,
                PageRequest.of(1, 5, Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("id"))));
    }

    @Test
    void noRatingsIsNullNotZeroStars() {
        when(tours.findById(1L)).thenReturn(Optional.of(Tour.builder().agencia(Agency.builder().build()).build()));
        when(reviews.findByReservaHorarioTourIdAndReservaEstadoAndPublishedTrue(eq(1L), eq(ReservationStatus.COMPLETED), any()))
                .thenReturn(Page.empty());
        var result = service.list(1L, 0, 5);
        assertNull(result.averageRating());
        assertEquals(0, result.totalElements());
        assertTrue(result.content().isEmpty());
    }

    @Test
    void inactiveTourOrAgencyDoesNotExposeReviews() {
        var agency = Agency.builder().estado(AgencyStatus.INACTIVE).build();
        var tour = Tour.builder().agencia(agency).build();
        when(tours.findById(1L)).thenReturn(Optional.of(tour));
        assertThrows(ResourceNotFoundException.class, () -> service.list(1L, 0, 5));
        agency.setEstado(AgencyStatus.ACTIVE);
        tour.setEstado(TourStatus.INACTIVE);
        assertThrows(ResourceNotFoundException.class, () -> service.list(1L, 0, 5));
        verifyNoInteractions(reviews);
    }

    @Test
    void unknownTourIsNotAnEmptySuccessfulList() {
        assertThrows(ResourceNotFoundException.class, () -> service.list(99L, 0, 5));
        verifyNoInteractions(reviews);
    }
}
