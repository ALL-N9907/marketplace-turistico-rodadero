package com.edu.unimag.rodatours.security;

import com.edu.unimag.rodatours.domain.entities.Reservation;
import com.edu.unimag.rodatours.domain.repositories.*;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component("accessPolicy")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessPolicy {
    private final TourRepository tours;
    private final TourScheduleRepository schedules;
    private final ReservationRepository reservations;
    private final GuideRepository guides;
    private final ReviewRepository reviews;

    public ApiPrincipal current() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ApiPrincipal actor) || !actor.enabled()) {
            throw new AccessDeniedException("Autenticación de usuario requerida.");
        }
        return actor;
    }

    public Long currentTouristId() {
        ApiPrincipal actor = current();
        if (actor.role() != AccountRole.TOURIST || actor.touristId() == null) {
            throw new AccessDeniedException("Se requiere una cuenta de turista.");
        }
        return actor.touristId();
    }

    public boolean tourist(Long id) {
        return id != null && current().role() == AccountRole.TOURIST && id.equals(current().touristId());
    }

    public boolean agency(Long id) {
        ApiPrincipal actor = current();
        return id != null && actor.role() == AccountRole.AGENCY_OPERATOR && id.equals(actor.agencyId());
    }

    public boolean tour(Long id) {
        return agency(tours.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tour no encontrado.")).getAgencia().getId());
    }

    public boolean schedule(Long id) {
        return tour(schedules.findById(id).orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado.")).getTour().getId());
    }

    public boolean guide(Long id) {
        return agency(guides.findById(id).orElseThrow(() -> new ResourceNotFoundException("Guía no encontrado.")).getAgencia().getId());
    }

    private Reservation reservation(Long id) {
        return reservations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada."));
    }

    public boolean operateReservation(Long id) {
        return agency(reservation(id).getHorario().getTour().getAgencia().getId());
    }

    public boolean cancelReservation(Long id) {
        Reservation reservation = reservation(id);
        return tourist(reservation.getTurista().getId()) || agency(reservation.getHorario().getTour().getAgencia().getId());
    }

    public boolean touristReservation(Long id) {
        return tourist(reservation(id).getTurista().getId());
    }

    public boolean review(Long id) {
        var review = reviews.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada."));
        return tourist(review.getReserva().getTurista().getId());
    }

    public boolean readReservation(Long id) {
        Reservation reservation = reservation(id);
        ApiPrincipal actor = current();
        return tourist(reservation.getTurista().getId())
                || agency(reservation.getHorario().getTour().getAgencia().getId())
                || (actor.role() == AccountRole.GUIDE && reservation.getHorario().getGuia() != null
                && Objects.equals(actor.guideId(), reservation.getHorario().getGuia().getId()));
    }

    public boolean completeReservation(Long id) {
        Reservation reservation = reservation(id);
        ApiPrincipal actor = current();
        return actor.role() == AccountRole.GUIDE && actor.guideId() != null
                && reservation.getHorario().getGuia() != null
                && actor.guideId().equals(reservation.getHorario().getGuia().getId());
    }

    public boolean listReservations(Long touristId, Long scheduleId) {
        return touristId != null && tourist(touristId) || scheduleId != null && schedule(scheduleId);
    }
}
