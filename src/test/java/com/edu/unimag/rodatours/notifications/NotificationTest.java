package com.edu.unimag.rodatours.notifications;

import com.edu.unimag.rodatours.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationTest {
    @Test
    void notifiesTouristAndAgencyOperatorsWithoutDuplicatingRecipients() {
        var notifications = mock(NotificationRepository.class);
        var accounts = mock(StaffAccountRepository.class);
        var operator = new StaffAccount();
        operator.setEmail("operator@example.com");
        when(accounts.findByAgencyIdAndRoleAndEnabledTrue(10L, AccountRole.AGENCY_OPERATOR))
                .thenReturn(List.of(operator, operator));
        new ReservationNotificationListener(notifications, accounts)
                .onReservationChanged(new ReservationChanged(1L, "tourist@example.com", 10L, "CONFIRMED"));
        var captor = org.mockito.ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).save(captor.capture());
        assertEquals(List.of("tourist@example.com", "operator@example.com"),
                captor.getAllValues().stream().map(Notification::getRecipientEmail).toList());
        assertTrue(captor.getValue().getMessage().contains("CONFIRMED"));
    }

    @Test
    void successfulMailRecordsDelivery() {
        var notifications = mock(NotificationRepository.class);
        var sender = mock(JavaMailSender.class);
        var notification = notification();
        when(notifications.findTop20ByEmailSentAtIsNullAndDeliveryAttemptsLessThanAndNextAttemptAtLessThanEqualOrderByIdAsc(eq(5), any()))
                .thenReturn(List.of(notification));
        var dispatcher = new MailNotificationDispatcher(notifications, sender);
        ReflectionTestUtils.setField(dispatcher, "from", "no-reply@example.com");
        dispatcher.dispatch();
        assertNotNull(notification.getEmailSentAt());
        assertEquals(1, notification.getDeliveryAttempts());
        verify(sender).send(any(SimpleMailMessage.class));
    }

    @Test
    void smtpFailureSchedulesRetryWithoutThrowingOrMarkingSent() {
        var notifications = mock(NotificationRepository.class);
        var sender = mock(JavaMailSender.class);
        var notification = notification();
        when(notifications.findTop20ByEmailSentAtIsNullAndDeliveryAttemptsLessThanAndNextAttemptAtLessThanEqualOrderByIdAsc(eq(5), any()))
                .thenReturn(List.of(notification));
        doThrow(new MailSendException("Temporary error")).when(sender).send(any(SimpleMailMessage.class));
        var dispatcher = new MailNotificationDispatcher(notifications, sender);
        ReflectionTestUtils.setField(dispatcher, "from", "no-reply@example.com");
        assertDoesNotThrow(dispatcher::dispatch);
        assertNull(notification.getEmailSentAt());
        assertEquals(1, notification.getDeliveryAttempts());
        assertTrue(notification.getNextAttemptAt().isAfter(Instant.now()));
        verify(notifications).save(notification);
    }

    private Notification notification() {
        var notification = new Notification();
        notification.setRecipientEmail("tourist@example.com");
        notification.setSubject("Reserva");
        notification.setMessage("CONFIRMED");
        return notification;
    }
}
