package com.edu.unimag.rodatours.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "rodatours.notifications.mail-enabled", havingValue = "true")
@RequiredArgsConstructor
public class MailNotificationDispatcher {
    private final NotificationRepository notifications;
    private final JavaMailSender mailSender;
    @Value("${rodatours.notifications.from}") private String from;

    @Scheduled(fixedDelayString = "${rodatours.notifications.poll-ms:30000}")
    @Transactional
    public void dispatch() {
        for (Notification notification : notifications
                .findTop20ByEmailSentAtIsNullAndDeliveryAttemptsLessThanAndNextAttemptAtLessThanEqualOrderByIdAsc(5, Instant.now())) {
            notification.setDeliveryAttempts(notification.getDeliveryAttempts() + 1);
            var message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(notification.getRecipientEmail());
            message.setSubject(notification.getSubject());
            message.setText(notification.getMessage());
            try {
                mailSender.send(message);
                notification.setEmailSentAt(Instant.now());
            } catch (MailException exception) {
                // No se guardan mensajes del proveedor: pueden contener direcciones o credenciales.
                notification.setNextAttemptAt(Instant.now().plusSeconds(60L * (1L << notification.getDeliveryAttempts())));
            }
            notifications.save(notification);
        }
    }
}
