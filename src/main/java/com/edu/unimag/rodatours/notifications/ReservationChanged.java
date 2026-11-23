package com.edu.unimag.rodatours.notifications;

import com.edu.unimag.rodatours.domain.entities.Reservation;

public record ReservationChanged(Long reservationId, String touristEmail, Long agencyId, String status) {
    public static ReservationChanged from(Reservation reservation) {
        return new ReservationChanged(reservation.getId(), reservation.getTurista().getCorreoElectronico(),
                reservation.getHorario().getTour().getAgencia().getId(), reservation.getEstado().name());
    }
}
