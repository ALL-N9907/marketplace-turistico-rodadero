package com.edu.unimag.rodatours.history;

import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.security.ApiPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReservationHistoryListener {
    private final ReservationHistoryRepository history;
    private final ReservationRepository reservations;

    /** Síncrono y obligatorio en la transacción de la reserva: un rollback revierte ambos. */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(ReservationStateChanged event) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var actor = authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof ApiPrincipal principal ? principal : null;
        history.save(new ReservationHistoryEntry(reservations.getReferenceById(event.reservationId()), event,
                actor == null ? "SYSTEM" : actor.role().name(), actor == null ? null : actor.email()));
    }
}
