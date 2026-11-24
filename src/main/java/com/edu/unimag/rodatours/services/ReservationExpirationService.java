package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.history.ReservationStateChanged;
import com.edu.unimag.rodatours.notifications.ReservationChanged;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationExpirationService {
    private final ReservationRepository reservations;
    private final ApplicationEventPublisher events;
    @Transactional
    public void expire(Long id, LocalDateTime cutoff) {
        var r=reservations.findByIdForUpdate(id).orElse(null);
        if(r==null||r.getEstado()!=ReservationStatus.SCHEDULED||r.getFechaReserva()==null||!r.getFechaReserva().isBefore(cutoff))return;
        r.cancel("Expirada por superar el plazo configurado de confirmación.");reservations.save(r);
        // SCHEDULED sólo retenía cupos: no se incrementa el stock físico del horario.
        events.publishEvent(new ReservationStateChanged(r.getId(),ReservationStatus.SCHEDULED,ReservationStatus.CANCELLED,LocalDateTime.now(),r.getMotivoCancelacion()));
        events.publishEvent(ReservationChanged.from(r));
    }
}
