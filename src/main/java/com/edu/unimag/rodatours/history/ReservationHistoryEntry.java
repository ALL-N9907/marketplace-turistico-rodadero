package com.edu.unimag.rodatours.history;

import com.edu.unimag.rodatours.domain.entities.Reservation;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation_history", indexes = @Index(name = "ix_history_reservation_time", columnList = "reservation_id,occurred_at,id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationHistoryEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, updatable = false)
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 15, updatable = false)
    private ReservationStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15, updatable = false)
    private ReservationStatus status;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "actor_role", nullable = false, length = 30, updatable = false)
    private String actorRole;

    // Se conserva internamente para auditoría, nunca se devuelve en la respuesta al viajero.
    @Column(name = "actor_email", length = 255, updatable = false)
    private String actorEmail;

    @Column(name = "reason", columnDefinition = "TEXT", updatable = false)
    private String reason;

    public ReservationHistoryEntry(Reservation reservation, ReservationStateChanged event,
                                   String actorRole, String actorEmail) {
        this.reservation = reservation;
        this.previousStatus = event.previousStatus();
        this.status = event.status();
        this.occurredAt = event.occurredAt();
        this.reason = event.reason();
        this.actorRole = actorRole;
        this.actorEmail = actorEmail;
    }
}
