package com.edu.unimag.rodatours.payments;

import com.edu.unimag.rodatours.services.exceptions.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "rodatours.payments.enabled", havingValue = "true")
@RequiredArgsConstructor
public class PaymentWebhookDispatcher {
    private final PaymentWebhookRepository webhooks;
    private final PaymentWebhookProcessor processor;
    @Scheduled(fixedDelay = 10000)
    public void dispatch() {
        for (var event : webhooks.findTop10ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc("PENDING", Instant.now())) {
            try { processor.process(event.getId()); }
            catch (RuntimeException e) { processor.failed(event.getId(), e instanceof ValidationException || e instanceof ResourceNotFoundException); }
        }
    }
}
