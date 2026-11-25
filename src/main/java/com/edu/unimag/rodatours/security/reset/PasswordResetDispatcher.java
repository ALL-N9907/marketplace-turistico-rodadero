package com.edu.unimag.rodatours.security.reset;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "rodatours.password-reset.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
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
            String stage = "PREPARACION";
            try {
                var message = new SimpleMailMessage(); message.setFrom(from); message.setTo(reset.getEmail());
                message.setSubject("Recupera tu acceso a Rodatours");
                message.setText("Solicitaste cambiar tu contraseña. El enlace vence en 30 minutos y es de un solo uso.\n"
                        + properties.getFrontendUrl().replaceAll("/+$", "") + "/recuperar#token=" + codec.decrypt(reset.getEncryptedToken())
                        + "\nSi no lo solicitaste, ignora este mensaje.");
                stage = "SMTP";
                mail.send(message); reset.setSentAt(Instant.now()); reset.setEncryptedToken(null);
                log.info("Correo de recuperacion aceptado por SMTP: solicitudId={}, intento={}", reset.getId(), reset.getAttempts());
            } catch (RuntimeException e) {
                reset.setNextAttemptAt(Instant.now().plusSeconds(60L * (1L << reset.getAttempts())));
                // Do not log exception messages/stack traces: providers can include credentials,
                // recipients or the message body (which contains the recovery token).
                log.warn("Fallo de correo de recuperacion: solicitudId={}, intento={}/5, etapa={}, categoria={}, proximoIntento={}, agotado={}",
                        reset.getId(), reset.getAttempts(), stage, failureCategory(e, stage),
                        reset.getNextAttemptAt(), reset.getAttempts() >= 5);
            }
            resets.save(reset);
        }
    }

    private static String failureCategory(RuntimeException failure, String stage) {
        if ("PREPARACION".equals(stage)) return "PREPARACION_O_CIFRADO";
        Throwable cause = failure;
        for (int depth = 0; cause != null && depth < 10; depth++, cause = cause.getCause()) {
            if (cause instanceof MailAuthenticationException || cause instanceof jakarta.mail.AuthenticationFailedException)
                return "AUTENTICACION_SMTP";
            if (cause instanceof javax.net.ssl.SSLException) return "TLS_SMTP";
            if (cause instanceof java.net.SocketTimeoutException) return "TIEMPO_AGOTADO_SMTP";
            if (cause instanceof java.net.UnknownHostException) return "DNS_SMTP";
            if (cause instanceof java.net.ConnectException) return "CONEXION_SMTP";
            if (cause instanceof jakarta.mail.SendFailedException) return "RECHAZO_SMTP";
        }
        return failure instanceof MailSendException ? "ENVIO_SMTP" : "ERROR_SMTP";
    }
}
