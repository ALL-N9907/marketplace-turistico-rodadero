package com.edu.unimag.rodatours.payments;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentWebhookProcessor {
    private final PaymentWebhookRepository webhooks;
    private final PaymentProvider provider;
    private final PaymentService payments;

    @Transactional
    public void process(Long id) {
        var event = webhooks.lockById(id).orElseThrow();
        if (!"PENDING".equals(event.getStatus()) || event.getNextAttemptAt().isAfter(Instant.now())) return;
        payments.apply(provider.get(event.getProviderPaymentId()));
        event.setStatus("DONE"); event.setOutcome("VERIFIED"); event.setAttempts(event.getAttempts() + 1);
        webhooks.save(event);
    }
    @Transactional
    public void failed(Long id, boolean terminal) {
        var event = webhooks.lockById(id).orElseThrow();
        if (!"PENDING".equals(event.getStatus())) return;
        event.setAttempts(event.getAttempts() + 1);
        event.setOutcome(terminal ? "INVALID_OR_UNKNOWN_REFERENCE" : "PROVIDER_OR_PROCESSING_ERROR");
        if (terminal || event.getAttempts() >= 5) event.setStatus("FAILED");
        else event.setNextAttemptAt(Instant.now().plusSeconds(60L * (1L << event.getAttempts())));
        webhooks.save(event);
    }
}
