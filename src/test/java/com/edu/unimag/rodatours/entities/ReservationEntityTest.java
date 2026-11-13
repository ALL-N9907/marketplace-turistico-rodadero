package com.edu.unimag.rodatours.entities;

import com.edu.unimag.rodatours.entities.enums.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReservationEntityTest {

    @Test
    @DisplayName("PrePersist asigna fechaReserva, estado SCHEDULED y codigoQr si son nulos")
    void testPrePersist() {
        Reservation reservation = Reservation.builder()
                .fechaInicio(LocalDateTime.now().plusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(1).plusHours(3))
                .cantidadPersonas(2)
                .precioTotal(new BigDecimal("150000.00"))
                .build();

        assertNull(reservation.getFechaReserva());
        assertNull(reservation.getCodigoQr());

        reservation.prePersist();

        assertNotNull(reservation.getFechaReserva());
        assertNotNull(reservation.getFechaModificacion());
        assertNotNull(reservation.getCodigoQr());
        assertEquals(ReservationStatus.SCHEDULED, reservation.getEstado());
    }

    @Test
    @DisplayName("confirm() solo tiene éxito desde estado SCHEDULED")
    void testConfirm() {
        Reservation reservation = Reservation.builder()
                .estado(ReservationStatus.SCHEDULED)
                .build();

        reservation.confirm();
        assertEquals(ReservationStatus.CONFIRMED, reservation.getEstado());
        assertNotNull(reservation.getFechaModificacion());

        assertThrows(IllegalStateException.class, reservation::confirm);
    }

    @Test
    @DisplayName("cancel() requiere motivo obligatorio y solo desde SCHEDULED o CONFIRMED")
    void testCancel() {
        Reservation scheduled = Reservation.builder()
                .estado(ReservationStatus.SCHEDULED)
                .build();

        assertThrows(IllegalArgumentException.class, () -> scheduled.cancel("   "));
        assertThrows(IllegalArgumentException.class, () -> scheduled.cancel(null));

        scheduled.cancel("Cambio de planes de viaje");
        assertEquals(ReservationStatus.CANCELLED, scheduled.getEstado());
        assertEquals("Cambio de planes de viaje", scheduled.getMotivoCancelacion());

        assertThrows(IllegalStateException.class, () -> scheduled.cancel("Otro motivo"));
    }

    @Test
    @DisplayName("complete() y markNoShow() solo permitidas desde CONFIRMED")
    void testCompleteAndNoShow() {
        Reservation completedRes = Reservation.builder()
                .estado(ReservationStatus.CONFIRMED)
                .build();

        completedRes.complete("Excelente experiencia");
        assertEquals(ReservationStatus.COMPLETED, completedRes.getEstado());
        assertTrue(completedRes.getObservaciones().contains("Excelente experiencia"));

        Reservation noShowRes = Reservation.builder()
                .estado(ReservationStatus.CONFIRMED)
                .build();

        noShowRes.markNoShow();
        assertEquals(ReservationStatus.NO_SHOW, noShowRes.getEstado());

        Reservation scheduledRes = Reservation.builder()
                .estado(ReservationStatus.SCHEDULED)
                .build();

        assertThrows(IllegalStateException.class, () -> scheduledRes.complete(null));
        assertThrows(IllegalStateException.class, scheduledRes::markNoShow);
    }
}
