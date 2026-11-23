package com.edu.unimag.rodatours.notifications;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_recipient", columnList = "recipient_email,created_at"),
        @Index(name = "idx_notification_delivery", columnList = "email_sent_at,next_attempt_at")})
@Getter
@Setter
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;
    @Column(nullable = false)
    private Long reservationId;
    @Column(nullable = false, length = 150)
    private String subject;
    @Column(nullable = false, length = 1000)
    private String message;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    private Instant readAt;
    @Column(name = "email_sent_at")
    private Instant emailSentAt;
    @Column(nullable = false)
    private int deliveryAttempts;
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();
    @Version
    private Long version;
}
