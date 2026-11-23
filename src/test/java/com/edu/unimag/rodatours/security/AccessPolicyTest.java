package com.edu.unimag.rodatours.security;

import com.edu.unimag.rodatours.domain.entities.*;
import com.edu.unimag.rodatours.domain.repositories.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessPolicyTest {
    @Mock TourRepository tours;
    @Mock TourScheduleRepository schedules;
    @Mock ReservationRepository reservations;
    @Mock GuideRepository guides;
    @Mock ReviewRepository reviews;
    @InjectMocks AccessPolicy policy;
    private Reservation reservation;

    @BeforeEach
    void setup() {
        var agency = Agency.builder().id(10L).build();
        var tour = Tour.builder().id(1L).agencia(agency).build();
        var guide = Guide.builder().id(20L).agencia(agency).build();
        var schedule = TourSchedule.builder().id(30L).tour(tour).guia(guide).build();
        reservation = Reservation.builder().id(40L).horario(schedule).turista(Tourist.builder().id(50L).build()).build();
    }

    private void actor(AccountRole role, Long agencyId, Long touristId, Long guideId) {
        var principal = new ApiPrincipal("user@example.com", "hash", role, agencyId, touristId, guideId, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void operatorCannotManageAnotherAgencysTour() {
        when(tours.findById(1L)).thenReturn(Optional.of(reservation.getHorario().getTour()));
        actor(AccountRole.AGENCY_OPERATOR, 11L, null, null);
        assertFalse(policy.tour(1L));
        actor(AccountRole.AGENCY_OPERATOR, 10L, null, null);
        assertTrue(policy.tour(1L));
    }

    @Test
    void touristCanOnlyReadAndCancelOwnReservationNotConfirmIt() {
        when(reservations.findById(40L)).thenReturn(Optional.of(reservation));
        actor(AccountRole.TOURIST, null, 50L, null);
        assertTrue(policy.readReservation(40L));
        assertTrue(policy.cancelReservation(40L));
        assertFalse(policy.operateReservation(40L));
        actor(AccountRole.TOURIST, null, 51L, null);
        assertFalse(policy.readReservation(40L));
        assertFalse(policy.cancelReservation(40L));
    }

    @Test
    void onlyAssignedGuideCanCompleteAndCannotCancel() {
        when(reservations.findById(40L)).thenReturn(Optional.of(reservation));
        actor(AccountRole.GUIDE, 10L, null, 20L);
        assertTrue(policy.completeReservation(40L));
        assertTrue(policy.readReservation(40L));
        assertFalse(policy.cancelReservation(40L));
        actor(AccountRole.GUIDE, 10L, null, 21L);
        assertFalse(policy.completeReservation(40L));
        assertFalse(policy.readReservation(40L));
    }

    @Test
    void unfilteredReservationListsAreNotAvailableToOperatorsOrTourists() {
        actor(AccountRole.AGENCY_OPERATOR, 10L, null, null);
        assertFalse(policy.listReservations(null, null));
        actor(AccountRole.TOURIST, null, 50L, null);
        assertFalse(policy.listReservations(null, null));
        assertTrue(policy.listReservations(50L, null));
        assertFalse(policy.listReservations(51L, null));
    }
}
