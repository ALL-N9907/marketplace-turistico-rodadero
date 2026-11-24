package com.edu.unimag.rodatours.payments;

import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.services.exceptions.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository payments;
    private final ReservationRepository reservations;
    private final PaymentProvider provider;
    private final PaymentProperties properties;

    @Transactional(readOnly = true)
    public PaymentView get(Long reservationId) {
        return view(payments.findByReservationId(reservationId).orElseThrow(() -> new ResourceNotFoundException("No hay un pago iniciado para esta reserva.")));
    }

    @Transactional(noRollbackFor = PaymentProviderException.class)
    public PaymentView checkout(Long reservationId) {
        properties.requireConfigured();
        var reservation = reservations.findByIdForUpdate(reservationId).orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada."));
        if (reservation.getEstado() != ReservationStatus.CONFIRMED || !reservation.getFechaInicio().isAfter(LocalDateTime.now()))
            throw new ConflictException("Sólo se inicia un pago para una reserva confirmada cuya salida no haya empezado.");
        var payment = payments.findByReservationId(reservationId).orElseGet(() -> {
            var p = new Payment(); p.setReservationId(reservationId); p.setReference(UUID.randomUUID().toString()); p.setAmount(reservation.getPrecioTotal());
            return payments.saveAndFlush(p);
        });
        payment = payments.lockByReference(payment.getReference()).orElseThrow();
        if (Set.of("APPROVED", "REFUNDED", "PARTIALLY_REFUNDED", "CHARGED_BACK", "REVIEW_REQUIRED").contains(payment.getStatus()))
            throw new ConflictException("El pago ya fue procesado o requiere revisión administrativa.");
        if (payment.getCheckoutUrl() != null && payment.getCheckoutExpiresAt().isAfter(Instant.now())) return view(payment);
        // No se renueva automáticamente una preferencia vencida: evita generar dos checkouts pagables.
        if (payment.getPreferenceId() != null) throw new ConflictException("El checkout venció. Concilia el pago y solicita revisión antes de iniciar otro.");
        if ("CHECKOUT_UNKNOWN".equals(payment.getStatus())) throw new ConflictException("Una solicitud anterior tuvo un resultado incierto. Concíliala antes de solicitar revisión; no se creará otro checkout automáticamente.");
        payment.setStatus("CHECKOUT_UNKNOWN"); payments.saveAndFlush(payment);
        var checkout = provider.create(payment.getReference(), reservationId, payment.getAmount());
        if (!PaymentProperties.sandboxUrl(checkout.sandboxUrl())) throw new PaymentProviderException();
        payment.setPreferenceId(checkout.id()); payment.setCheckoutUrl(checkout.sandboxUrl());
        payment.setCheckoutExpiresAt(Instant.now().plusSeconds(1800)); payment.setStatus("CHECKOUT_READY");
        return view(payments.save(payment));
    }

    @Transactional
    public PaymentView reconcile(Long reservationId) {
        properties.requireConfigured();
        var p = payments.findByReservationId(reservationId).orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado."));
        p = payments.lockByReference(p.getReference()).orElseThrow();
        var ids = provider.search(p.getReference());
        // Cada recurso se verifica de nuevo; nunca se confía en el estado de una URL de retorno.
        for (String id : ids) {
            var remote=provider.get(id);
            if(!p.getReference().equals(remote.reference()))throw new ValidationException("La búsqueda devolvió un pago de otra referencia.");
            apply(remote);
        }
        p.setCheckedAt(Instant.now());
        return view(payments.save(p));
    }

    @Transactional
    public void apply(PaymentProvider.RemotePayment remote) {
        properties.requireConfigured();
        var payment = payments.lockByReference(remote.reference()).orElseThrow(() -> new ResourceNotFoundException("Referencia de pago no registrada."));
        if (remote.liveMode() || !properties.getCollectorId().equals(remote.collectorId()) || !"COP".equals(remote.currency())
                || payment.getAmount().compareTo(remote.amount()) != 0 || remote.updatedAt() == null
                || remote.refunded() == null || remote.refunded().signum() < 0 || remote.refunded().compareTo(remote.amount()) > 0)
            throw new ValidationException("El pago no corresponde al entorno, vendedor, moneda o importe esperado.");
        if (payment.getProviderPaymentId() != null && !payment.getProviderPaymentId().equals(remote.id())
                && Set.of("APPROVED", "REFUNDED", "PARTIALLY_REFUNDED", "CHARGED_BACK", "REVIEW_REQUIRED").contains(payment.getStatus())) {
            if ("approved".equals(remote.status())) { payment.setDuplicateDetected(true); payment.setStatus("REVIEW_REQUIRED"); payments.save(payment); }
            return;
        }
        if (payment.getProviderPaymentId() != null && payment.getProviderPaymentId().equals(remote.id())
                && payment.getProviderUpdatedAt() != null && payment.getProviderUpdatedAt().isAfter(remote.updatedAt())) return;
        String state = switch (remote.status()) {
            case "approved" -> remote.refunded().compareTo(remote.amount()) == 0 ? "REFUNDED" : remote.refunded().signum() > 0 ? "PARTIALLY_REFUNDED" : "APPROVED";
            case "pending", "in_process", "authorized" -> "PENDING";
            case "rejected" -> "REJECTED";
            case "cancelled" -> "CANCELLED";
            case "refunded" -> "REFUNDED";
            case "charged_back" -> "CHARGED_BACK";
            default -> throw new ValidationException("Estado del proveedor no reconocido; requiere revisión.");
        };
        if (!payment.isDuplicateDetected()) payment.setStatus(state);
        payment.setProviderPaymentId(remote.id()); payment.setProviderUpdatedAt(remote.updatedAt()); payment.setCheckedAt(Instant.now());
        payments.save(payment);
        // Deliberadamente no confirma, cancela ni reabre la reserva; pago y operación son independientes.
    }

    private PaymentView view(Payment p) {
        String url = p.getCheckoutExpiresAt() != null && p.getCheckoutExpiresAt().isAfter(Instant.now())
                && !Set.of("APPROVED", "REVIEW_REQUIRED", "REFUNDED", "PARTIALLY_REFUNDED", "CHARGED_BACK").contains(p.getStatus()) ? p.getCheckoutUrl() : null;
        return new PaymentView(p.getId(), p.getReservationId(), p.getAmount(), "COP", p.getStatus(), url, p.getCheckedAt(), p.isDuplicateDetected(), true);
    }
    public record PaymentView(Long id, Long reservationId, BigDecimal amount, String currency, String status,
                              String checkoutUrl, Instant checkedAt, boolean duplicateDetected, boolean sandbox) {}
}
