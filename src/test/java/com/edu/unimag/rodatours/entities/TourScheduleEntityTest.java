package com.edu.unimag.rodatours.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class TourScheduleEntityTest {

    @Test
    @DisplayName("PrePersist inicializa cuposDisponibles con cuposTotales si es nulo")
    void testPrePersist() {
        TourSchedule schedule = TourSchedule.builder()
                .cuposTotales(20)
                .build();

        assertNull(schedule.getCuposDisponibles());
        schedule.prePersist();
        assertEquals(20, schedule.getCuposDisponibles());
    }

    @Test
    @DisplayName("Cálculo de hora de fin a partir de la duración del tour")
    void testCalculateEndDateTime() {
        Tour tour = Tour.builder()
                .duracionHoras(new BigDecimal("2.5"))
                .build();

        TourSchedule schedule = TourSchedule.builder()
                .tour(tour)
                .fechaTour(LocalDate.of(2026, 12, 1))
                .horaInicio(LocalTime.of(9, 0))
                .cuposTotales(10)
                .cuposDisponibles(10)
                .build();

        LocalDateTime end = schedule.calculateEndDateTime();
        assertNotNull(end);
        assertEquals(LocalDateTime.of(2026, 12, 1, 11, 30), end);
    }

    @Test
    @DisplayName("Descuento y liberación de cupos con validaciones")
    void testSpotDiscountAndRelease() {
        TourSchedule schedule = TourSchedule.builder()
                .cuposTotales(10)
                .cuposDisponibles(10)
                .build();

        assertTrue(schedule.hasAvailability(4));
        schedule.discountSpots(4);
        assertEquals(6, schedule.getCuposDisponibles());

        assertFalse(schedule.hasAvailability(7));
        assertThrows(IllegalStateException.class, () -> schedule.discountSpots(7));

        schedule.releaseSpots(3);
        assertEquals(9, schedule.getCuposDisponibles());

        schedule.releaseSpots(5);
        assertEquals(10, schedule.getCuposDisponibles());
    }
}
