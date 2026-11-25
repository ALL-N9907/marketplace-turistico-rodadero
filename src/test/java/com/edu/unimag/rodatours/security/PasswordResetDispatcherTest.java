package com.edu.unimag.rodatours.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.edu.unimag.rodatours.security.reset.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetDispatcherTest {
    @Mock PasswordResetRepository resets;
    @Mock ResetTokenCodec codec;
    @Mock JavaMailSender mail;
    PasswordResetDispatcher dispatcher;
    PasswordReset reset;
    Logger logger;
    ListAppender<ILoggingEvent> logs;

    @BeforeEach void setup() {
        var properties = new PasswordResetProperties();
        properties.setFrontendUrl("https://rodatours.example");
        dispatcher = new PasswordResetDispatcher(resets, properties, codec, mail);
        ReflectionTestUtils.setField(dispatcher, "mailEnabled", true);
        ReflectionTestUtils.setField(dispatcher, "from", "sender@example.com");
        reset = new PasswordReset();
        reset.setId(7L); reset.setEmail("private@example.com");
        reset.setEncryptedToken("encrypted-secret"); reset.setExpiresAt(Instant.now().plusSeconds(1800));
        when(resets.findTop10BySentAtIsNullAndConsumedAtIsNullAndEncryptedTokenIsNotNullAndAttemptsLessThanAndNextAttemptAtLessThanEqualOrderByIdAsc(eq(5), any()))
                .thenReturn(List.of(reset));
        logger = (Logger) LoggerFactory.getLogger(PasswordResetDispatcher.class);
        logs = new ListAppender<>(); logs.start(); logger.addAppender(logs);
    }

    @AfterEach void cleanup() { logger.detachAppender(logs); logs.stop(); }

    @Test void authenticationFailureIsLoggedWithoutSecretsAndRetried() {
        when(codec.decrypt(anyString())).thenReturn("private-token");
        doThrow(new MailAuthenticationException("private-password private@example.com private-token"))
                .when(mail).send(any(SimpleMailMessage.class));
        dispatcher.dispatch();
        assertNull(reset.getSentAt()); assertEquals(1, reset.getAttempts());
        assertNotNull(reset.getNextAttemptAt()); verify(resets).save(reset);
        assertSafeLog("AUTENTICACION_SMTP");
    }

    @Test void connectionFailureIsClassifiedThroughCause() {
        when(codec.decrypt(anyString())).thenReturn("private-token");
        doThrow(new MailSendException("private-password", new java.net.ConnectException("private@example.com")))
                .when(mail).send(any(SimpleMailMessage.class));
        dispatcher.dispatch();
        assertSafeLog("CONEXION_SMTP");
    }

    @Test void encryptionFailureDoesNotAttemptSmtp() {
        when(codec.decrypt(anyString())).thenThrow(new IllegalStateException("encrypted-secret"));
        dispatcher.dispatch();
        verifyNoInteractions(mail); assertSafeLog("PREPARACION_O_CIFRADO");
    }

    @Test void successfulSmtpAcceptanceClearsEncryptedToken() {
        when(codec.decrypt(anyString())).thenReturn("private-token");
        dispatcher.dispatch();
        assertNotNull(reset.getSentAt()); assertNull(reset.getEncryptedToken());
        assertSafeLog("aceptado por SMTP");
    }

    @Test void lastAttemptIsMarkedExhausted() {
        reset.setAttempts(4);
        when(codec.decrypt(anyString())).thenReturn("private-token");
        doThrow(new MailSendException("private-token")).when(mail).send(any(SimpleMailMessage.class));
        dispatcher.dispatch();
        assertEquals(5, reset.getAttempts()); assertSafeLog("agotado=true");
    }

    private void assertSafeLog(String expected) {
        assertEquals(1, logs.list.size());
        var event = logs.list.getFirst();
        String text = event.getFormattedMessage();
        assertTrue(text.contains(expected), text);
        for (String secret : List.of("private@example.com", "private-token", "private-password", "encrypted-secret"))
            assertFalse(text.contains(secret));
        assertNull(event.getThrowableProxy());
    }
}
