package com.edu.unimag.rodatours.payments;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentWebhookService {
    private final WebhookSignature signature;
    private final PaymentWebhookRepository webhooks;
    @Transactional
    public void accept(String paymentId, String requestId, String header) {
        var verified = signature.verify(paymentId, requestId, header);
        if (!verified.fresh() && !webhooks.existsByEventKey(verified.key())) throw new BadCredentialsException("Notificación vencida.");
        webhooks.insertIfAbsent(verified.key(), paymentId);
    }
}
