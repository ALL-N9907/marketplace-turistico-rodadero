package com.edu.unimag.rodatours.notifications;

import com.edu.unimag.rodatours.security.AccessPolicy;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;
    private final AccessPolicy access;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> mine(int page, int size) {
        return notifications.findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(access.current().email(),
                PageRequest.of(page, size)).map(this::response);
    }

    @Transactional
    public NotificationResponse read(Long id) {
        Notification notification = notifications.findByIdAndRecipientEmailIgnoreCase(id, access.current().email())
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada."));
        if (notification.getReadAt() == null) { notification.setReadAt(Instant.now()); }
        return response(notifications.save(notification));
    }

    private NotificationResponse response(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getReservationId(), notification.getSubject(),
                notification.getMessage(), notification.getCreatedAt(), notification.getReadAt());
    }

    public record NotificationResponse(Long id, Long reservationId, String subject, String message,
                                       Instant createdAt, Instant readAt) {}
}
