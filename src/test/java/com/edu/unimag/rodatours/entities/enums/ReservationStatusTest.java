package com.edu.unimag.rodatours.entities.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReservationStatusTest {

    @Test
    @DisplayName("SCHEDULED solo puede transicionar a CONFIRMED y CANCELLED")
    void testScheduledTransitions() {
        assertTrue(ReservationStatus.SCHEDULED.canTransitionTo(ReservationStatus.CONFIRMED));
        assertTrue(ReservationStatus.SCHEDULED.canTransitionTo(ReservationStatus.CANCELLED));
        assertFalse(ReservationStatus.SCHEDULED.canTransitionTo(ReservationStatus.COMPLETED));
        assertFalse(ReservationStatus.SCHEDULED.canTransitionTo(ReservationStatus.NO_SHOW));
        assertFalse(ReservationStatus.SCHEDULED.canTransitionTo(ReservationStatus.SCHEDULED));
        assertFalse(ReservationStatus.SCHEDULED.isTerminal());
    }

    @Test
    @DisplayName("CONFIRMED puede transicionar a CANCELLED, COMPLETED y NO_SHOW")
    void testConfirmedTransitions() {
        assertTrue(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.CANCELLED));
        assertTrue(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.COMPLETED));
        assertTrue(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.NO_SHOW));
        assertFalse(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.SCHEDULED));
        assertFalse(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.CONFIRMED));
        assertFalse(ReservationStatus.CONFIRMED.isTerminal());
    }

    @Test
    @DisplayName("CANCELLED, COMPLETED y NO_SHOW son terminales y no permiten transiciones")
    void testTerminalStates() {
        assertTrue(ReservationStatus.CANCELLED.isTerminal());
        assertTrue(ReservationStatus.COMPLETED.isTerminal());
        assertTrue(ReservationStatus.NO_SHOW.isTerminal());

        for (ReservationStatus target : ReservationStatus.values()) {
            assertFalse(ReservationStatus.CANCELLED.canTransitionTo(target));
            assertFalse(ReservationStatus.COMPLETED.canTransitionTo(target));
            assertFalse(ReservationStatus.NO_SHOW.canTransitionTo(target));
        }
    }
}
