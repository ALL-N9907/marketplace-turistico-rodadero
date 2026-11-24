package com.edu.unimag.rodatours.history;

import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import java.time.LocalDateTime;

/** Snapshot inmutable, emitido sólo después de aplicar una transición válida. */
public record ReservationStateChanged(Long reservationId, ReservationStatus previousStatus,
                                      ReservationStatus status, LocalDateTime occurredAt, String reason) {}
