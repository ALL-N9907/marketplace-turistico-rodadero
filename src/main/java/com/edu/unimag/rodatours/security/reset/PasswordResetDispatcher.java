package com.edu.unimag.rodatours.security.reset;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "rodatours.password-reset.enabled", havingValue = "true")
@RequiredArgsConstructor
public class PasswordResetDispatcher {
    private final PasswordResetRepository resets;
    private final PasswordResetProperties properties;
    private final ResetTokenCodec codec;
    private final JavaMailSender mail;
    @Value("${rodatours.notifications.from}") private String from;
    @Value("${rodatours.notifications.mail-enabled:false}") private boolean mailEnabled;

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void dispatch() {
        if (!mailEnabled) return;
        for (var reset : resets.findTop10BySentAtIsNullAndConsumedAtIsNullAndEncryptedTokenIsNotNullAndAttemptsLessThanAndNextAttemptAtLessThanEqualOrderByIdAsc(5, Instant.now())) {
            if (!reset.getExpiresAt().isAfter(Instant.now())) { reset.setEncryptedToken(null); resets.save(reset); continue; }
            reset.setAttempts(reset.getAttempts() + 1);
            try {
                var message = new SimpleMailMessage(); message.setFrom(from); message.setTo(reset.getEmail());
                message.setSubject("Recupera tu acceso a Rodatours");
                message.setText("Solicitaste cambiar tu contraseña. El enlace vence en 30 minutos y es de un solo uso.\n"
                        + properties.getFrontendUrl().replaceAll("/+$", "") + "/recuperar#token=" + codec.decrypt(reset.getEncryptedToken())
                        + "\nSi no lo solicitaste, ignora este mensaje.");
                mail.send(message); reset.setSentAt(Instant.now()); reset.setEncryptedToken(null);
            } catch (RuntimeException e) { reset.setNextAttemptAt(Instant.now().plusSeconds(60L * (1L << reset.getAttempts()))); }
            resets.save(reset);
        }
    }
}
