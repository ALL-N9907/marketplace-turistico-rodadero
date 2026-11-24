package com.edu.unimag.rodatours.repositories;

import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.ExperienceType;
import com.edu.unimag.rodatours.domain.entities.Reservation;
import com.edu.unimag.rodatours.domain.entities.Review;
import com.edu.unimag.rodatours.domain.entities.Tour;
import com.edu.unimag.rodatours.domain.entities.TourSchedule;
import com.edu.unimag.rodatours.domain.entities.Tourist;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

class ReviewRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

    @Autowired
    private ReviewRepository repository;

    @Test
    @DisplayName("Busca la reseña asociada a una reserva")
    void findByReservaId() {
        Reservation reserva = reserva("reseña");
        Review reseña = TestDataFactory.review(entityManager, reserva, 5);

        assertEquals(reseña.getId(), repository.findByReservaId(reserva.getId()).orElseThrow().getId());
        assertTrue(repository.existsByReservaId(reserva.getId()));
        assertEquals(5, repository.findByReservaId(reserva.getId()).orElseThrow().getCalificacion());
        assertTrue(repository.findByReservaId(999L).isEmpty());
    }

    @Test
    @DisplayName("Lista las reseñas de las reservas de un turista")
    void findByReservaTuristaId() {
        Agency agency = TestDataFactory.agency(entityManager, "resenas");
        ExperienceType type = TestDataFactory.experienceType(entityManager, "Playa");
        Tour tour = TestDataFactory.tour(entityManager, agency, type, "Resenado");
        TourSchedule schedule = TestDataFactory.schedule(entityManager, tour, LocalTime.of(9, 0), 10);
        Tourist turista = TestDataFactory.tourist(entityManager, "resenante");
        Tourist otro = TestDataFactory.tourist(entityManager, "otro-resenante");

        Reservation r1 = TestDataFactory.reservation(entityManager, turista, schedule,
                ReservationStatus.COMPLETED, LocalDateTime.of(TestDataFactory.FECHA_TOUR, LocalTime.of(9, 0)), 1);
        Reservation r2 = TestDataFactory.reservation(entityManager, turista, schedule,
                ReservationStatus.COMPLETED, LocalDateTime.of(TestDataFactory.FECHA_TOUR, LocalTime.of(11, 0)), 1);
        Reservation r3 = TestDataFactory.reservation(entityManager, otro, schedule,
                ReservationStatus.COMPLETED, LocalDateTime.of(TestDataFactory.FECHA_TOUR, LocalTime.of(13, 0)), 1);

        TestDataFactory.review(entityManager, r1, 5);
        TestDataFactory.review(entityManager, r2, 4);
        TestDataFactory.review(entityManager, r3, 3);

        assertEquals(2, repository.findByReservaTuristaId(turista.getId()).size());
        assertEquals(1, repository.findByReservaTuristaId(otro.getId()).size());
        assertTrue(repository.findByReservaTuristaId(999L).isEmpty());
    }

    private Reservation reserva(String key) {
        Agency agency = TestDataFactory.agency(entityManager, key);
        ExperienceType type = TestDataFactory.experienceType(entityManager, "Tipo " + key);
        Tour tour = TestDataFactory.tour(entityManager, agency, type, key);
        TourSchedule schedule = TestDataFactory.schedule(entityManager, tour, LocalTime.of(9, 0), 10);
        Tourist turista = TestDataFactory.tourist(entityManager, key);
        return TestDataFactory.reservation(entityManager, turista, schedule, ReservationStatus.COMPLETED,
                LocalDateTime.of(TestDataFactory.FECHA_TOUR, LocalTime.of(9, 0)), 1);
    }

    @Test
    void publicQueryPagesByTourAndOnlyCountsCompletedReservations() {
        Reservation completed = reserva("public-completed");
        TestDataFactory.review(entityManager, completed, 5);
        Reservation pending = TestDataFactory.reservation(entityManager, completed.getTurista(), completed.getHorario(),
                ReservationStatus.SCHEDULED, completed.getFechaInicio().plusHours(3), 1);
        TestDataFactory.review(entityManager, pending, 1);
        Reservation otherTour = reserva("other-tour");
        TestDataFactory.review(entityManager, otherTour, 2);
        entityManager.flush();
        var result = repository.findByReservaHorarioTourIdAndReservaEstado(completed.getHorario().getTour().getId(),
                ReservationStatus.COMPLETED, PageRequest.of(0, 5, Sort.by("id").descending()));
        assertEquals(1, result.getTotalElements());
        assertEquals(5, result.getContent().getFirst().getCalificacion());
        assertEquals(5.0, repository.averageByTour(completed.getHorario().getTour().getId(), ReservationStatus.COMPLETED));
    }
}
