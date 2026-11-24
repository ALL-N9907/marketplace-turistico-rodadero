package com.edu.unimag.rodatours.payments;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PaymentProvider {
    Checkout create(String reference, Long reservationId, BigDecimal amount);
    RemotePayment get(String id);
    List<String> search(String reference);
    record Checkout(String id, String sandboxUrl) {}
    record RemotePayment(String id, String reference, BigDecimal amount, String currency, String status,
                         boolean liveMode, String collectorId, BigDecimal refunded, Instant updatedAt) {}
}
