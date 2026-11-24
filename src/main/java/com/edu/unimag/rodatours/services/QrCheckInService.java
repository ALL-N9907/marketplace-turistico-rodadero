package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.security.AccessPolicy;
import com.edu.unimag.rodatours.security.AccountRole;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QrCheckInService {
    private static final String PREFIX = "rodatours:reservation:";
    private final ReservationRepository reservations;
    private final ReservationVoucherService vouchers;
    private final AccessPolicy access;

    /** Resuelve y canjea en una transacción; nunca ofrece una búsqueda pública por QR. */
    @Transactional
    public ReservationVoucherService.Voucher checkIn(String payload) {
        if (payload == null || !payload.startsWith(PREFIX)
                || !payload.substring(PREFIX.length()).matches("[A-Za-z0-9_-]{1,100}")) {
            throw new ValidationException("El contenido no es un QR válido de Rodatours.");
        }
        var actor = access.current();
        if (actor.role() != AccountRole.ADMIN && actor.role() != AccountRole.AGENCY_OPERATOR
                && actor.role() != AccountRole.GUIDE) {
            throw new org.springframework.security.access.AccessDeniedException("No puede validar reservas.");
        }
        var reservation = reservations.findByCodigoQrForUpdate(payload.substring(PREFIX.length()))
                .orElseThrow(QrCheckInService::unavailable);
        // No revelar siquiera el ID de una reserva de otra agencia o guía.
        if (actor.role() != AccountRole.ADMIN && !access.operateReservation(reservation.getId())
                && !access.completeReservation(reservation.getId())) {
            throw unavailable();
        }
        // Reutiliza las mismas validaciones de fecha, estado y canje único del endpoint por ID.
        return vouchers.checkIn(reservation.getId(), payload);
    }

    private static ResourceNotFoundException unavailable() {
        return new ResourceNotFoundException("Voucher no disponible para esta cuenta.");
    }
}
