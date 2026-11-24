package com.edu.unimag.rodatours.payments;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Getter @Setter
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private Long reservationId;
    @Column(nullable = false, unique = true, length = 36) private String reference;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 30) private String status = "NOT_STARTED";
    @Column(length = 150) private String preferenceId;
    @Column(length = 1500) private String checkoutUrl;
    private Instant checkoutExpiresAt;
    @Column(unique = true, length = 30) private String providerPaymentId;
    private Instant providerUpdatedAt;
    private Instant checkedAt;
    @Column(nullable = false) private boolean duplicateDetected;
    @Version private Long version;
}
