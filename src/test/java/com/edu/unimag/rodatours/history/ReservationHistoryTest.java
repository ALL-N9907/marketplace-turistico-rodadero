package com.edu.unimag.rodatours.history;

import com.edu.unimag.rodatours.domain.entities.Reservation;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.security.*;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationHistoryTest {
    @Mock ReservationHistoryRepository history;
    @Mock ReservationRepository reservations;
    private final LocalDateTime time = LocalDateTime.of(2026, 11, 23, 10, 30);

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void capturesActorAndStateSnapshotWithoutKeepingMutableReservationState() {
        var reservation = Reservation.builder().id(1L).estado(ReservationStatus.CONFIRMED).build();
        var actor = new ApiPrincipal("operador@example.com", "secret-hash", AccountRole.AGENCY_OPERATOR, 2L, null, null, true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities()));
        when(reservations.getReferenceById(1L)).thenReturn(reservation);
        var event = new ReservationStateChanged(1L, ReservationStatus.SCHEDULED, ReservationStatus.CONFIRMED, time, null);
        new ReservationHistoryListener(history, reservations).record(event);
        reservation.setEstado(ReservationStatus.CANCELLED);
        var saved = ArgumentCaptor.forClass(ReservationHistoryEntry.class);
        verify(history).save(saved.capture());
        assertEquals(ReservationStatus.SCHEDULED, saved.getValue().getPreviousStatus());
        assertEquals(ReservationStatus.CONFIRMED, saved.getValue().getStatus());
        assertEquals("AGENCY_OPERATOR", saved.getValue().getActorRole());
        assertEquals("operador@example.com", saved.getValue().getActorEmail());
        assertEquals(time, saved.getValue().getOccurredAt());
    }

    @Test
    void supportsSystemActorAndNullPreviousStateForCreation() {
        when(reservations.getReferenceById(1L)).thenReturn(Reservation.builder().id(1L).build());
        new ReservationHistoryListener(history, reservations).record(
                new ReservationStateChanged(1L, null, ReservationStatus.SCHEDULED, time, null));
        var saved = ArgumentCaptor.forClass(ReservationHistoryEntry.class);
        verify(history).save(saved.capture());
        assertEquals("SYSTEM", saved.getValue().getActorRole());
        assertNull(saved.getValue().getActorEmail());
        assertNull(saved.getValue().getPreviousStatus());
    }

    @Test
    void pagesChronologicallyAndDoesNotExposeActorEmail() {
        when(reservations.existsById(1L)).thenReturn(true);
        var event = new ReservationStateChanged(1L, ReservationStatus.CONFIRMED, ReservationStatus.CANCELLED, time, "Cambio de planes");
        var item = new ReservationHistoryEntry(Reservation.builder().id(1L).build(), event, "TOURIST", "private@example.com");
        when(history.findByReservationId(eq(1L), any())).thenReturn(new PageImpl<>(List.of(item)));
        var result = new ReservationHistoryService(history, reservations).list(1L, 0, 20);
        assertEquals("Cambio de planes", result.content().getFirst().reason());
        assertEquals("TOURIST", result.content().getFirst().actorRole());
        assertFalse(result.toString().contains("private@example.com"));
        verify(history).findByReservationId(1L, PageRequest.of(0, 20, Sort.by(Sort.Order.asc("occurredAt"), Sort.Order.asc("id"))));
    }

    @Test
    void missingReservationDoesNotReturnFalseEmptyHistory() {
        assertThrows(ResourceNotFoundException.class, () -> new ReservationHistoryService(history, reservations).list(9L, 0, 20));
        verifyNoInteractions(history);
    }
}
