package com.edu.unimag.rodatours.config;

import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.services.ReservationExpirationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReservationExpirationJob {
    private final ReservationRepository reservations;
    private final ReservationExpirationService expiration;
    @Value("${rodatours.reservations.pending-ttl-minutes:0}") private int ttl;
    @Scheduled(fixedDelay=60000)
    public void run(){
        if(ttl<=0)return;
        if(ttl>10080)throw new IllegalStateException("El plazo de pendientes no puede superar 7 días.");
        var cutoff=LocalDateTime.now().minusMinutes(ttl);
        for(var r:reservations.findTop50ByEstadoAndFechaReservaBeforeOrderByIdAsc(ReservationStatus.SCHEDULED,cutoff))expiration.expire(r.getId(),cutoff);
    }
}
