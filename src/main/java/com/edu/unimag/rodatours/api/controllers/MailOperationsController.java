package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.notifications.NotificationRepository;
import com.edu.unimag.rodatours.payments.PaymentWebhookRepository;
import com.edu.unimag.rodatours.services.exceptions.*;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/deliveries")
@PreAuthorize("hasRole('ADMIN')")
public class MailOperationsController {
    private final NotificationRepository notifications;
    private final PaymentWebhookRepository webhooks;
    @GetMapping("/mail")
    @Transactional(readOnly = true)
    public List<MailItem> mail(@RequestParam(defaultValue = "0") @Min(0) int page,
                               @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return notifications.findAll(PageRequest.of(page, size, Sort.by("id").descending())).map(n ->
                new MailItem(n.getId(), n.getReservationId(), n.getRecipientEmail(), n.getDeliveryAttempts(), n.getEmailSentAt(), n.getNextAttemptAt())).getContent();
    }
    @PostMapping("/mail/{id}/retry")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @Transactional
    public void retryMail(@PathVariable @Positive Long id) {
        var n = notifications.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Aviso no encontrado."));
        if (n.getEmailSentAt() != null) throw new ConflictException("El aviso ya fue enviado; no se duplicará el envío.");
        n.setDeliveryAttempts(0); n.setNextAttemptAt(Instant.now()); notifications.save(n);
    }
    @GetMapping("/webhooks")
    @Transactional(readOnly = true)
    public List<WebhookItem> webhooks(@RequestParam(defaultValue = "0") @Min(0) int page,
                                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return webhooks.findAllByOrderByIdDesc(PageRequest.of(page, size)).map(w ->
                new WebhookItem(w.getId(), w.getProviderPaymentId(), w.getStatus(), w.getAttempts(), w.getOutcome())).getContent();
    }
    @PostMapping("/webhooks/{id}/retry")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @Transactional
    public void retryWebhook(@PathVariable @Positive Long id) {
        var w = webhooks.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado."));
        if (!"FAILED".equals(w.getStatus())) throw new ConflictException("Sólo se reintentan eventos fallidos.");
        w.setStatus("PENDING"); w.setAttempts(0); w.setNextAttemptAt(Instant.now()); webhooks.save(w);
    }
    public record MailItem(Long id, Long reservationId, String recipient, int attempts, Instant sentAt, Instant nextAttemptAt) {}
    public record WebhookItem(Long id, String providerPaymentId, String status, int attempts, String outcome) {}
}
