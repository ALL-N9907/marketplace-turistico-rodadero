package com.edu.unimag.rodatours.payments;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "payment_webhooks")
@Getter @Setter
public class PaymentWebhook {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 64) private String eventKey;
    @Column(nullable = false, length = 30) private String providerPaymentId;
    @Column(nullable = false, length = 20) private String status = "PENDING";
    @Column(nullable = false) private int attempts;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant nextAttemptAt;
    @Column(length = 50) private String outcome;
    @Version private Long version;
}
