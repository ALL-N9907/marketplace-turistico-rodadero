package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.domain.entities.Reservation;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.security.*;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrCheckInServiceTest {
    @Mock ReservationRepository reservations;
    @Mock ReservationVoucherService vouchers;
    @Mock AccessPolicy access;
    @InjectMocks QrCheckInService service;
    private static final String PAYLOAD = "rodatours:reservation:unique-code";

    private void actor(AccountRole role) {
        when(access.current()).thenReturn(new ApiPrincipal("actor@example.com", "hash", role, 10L, null, 20L, true));
    }

    private void found() {
        when(reservations.findByCodigoQrForUpdate("unique-code"))
                .thenReturn(Optional.of(Reservation.builder().id(40L).build()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"wrong", "https://example.com", "rodatours:reservation:",
            "rodatours:reservation:code with spaces", "rodatours:reservation:abc:123"})
    void rejectsMalformedPayloadWithoutLookup(String payload) {
        assertThrows(ValidationException.class, () -> service.checkIn(payload));
        verifyNoInteractions(reservations, vouchers, access);
    }

    @Test
    void adminResolvesAndDelegatesToExistingRedeemRules() {
        actor(AccountRole.ADMIN);
        found();
        service.checkIn(PAYLOAD);
        verify(vouchers).checkIn(40L, PAYLOAD);
        verify(access, never()).operateReservation(any());
    }

    @Test
    void operatorMustOwnAgency() {
        actor(AccountRole.AGENCY_OPERATOR);
        found();
        when(access.operateReservation(40L)).thenReturn(true);
        service.checkIn(PAYLOAD);
        verify(vouchers).checkIn(40L, PAYLOAD);
    }

    @Test
    void guideMustBeAssigned() {
        actor(AccountRole.GUIDE);
        found();
        when(access.completeReservation(40L)).thenReturn(true);
        service.checkIn(PAYLOAD);
        verify(vouchers).checkIn(40L, PAYLOAD);
    }

    @Test
    void foreignAndUnknownCodesHaveSameResponseAndNeverRedeem() {
        actor(AccountRole.AGENCY_OPERATOR);
        found();
        var foreign = assertThrows(ResourceNotFoundException.class, () -> service.checkIn(PAYLOAD));
        when(reservations.findByCodigoQrForUpdate("unique-code")).thenReturn(Optional.empty());
        var unknown = assertThrows(ResourceNotFoundException.class, () -> service.checkIn(PAYLOAD));
        assertEquals(foreign.getMessage(), unknown.getMessage());
        verifyNoInteractions(vouchers);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TOURIST", "COORDINATOR"})
    void readersCannotUseQrAsAccessCredential(String role) {
        actor(AccountRole.valueOf(role));
        assertThrows(AccessDeniedException.class, () -> service.checkIn(PAYLOAD));
        verifyNoInteractions(reservations, vouchers);
    }
}
