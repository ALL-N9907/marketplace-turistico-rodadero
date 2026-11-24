package com.edu.unimag.rodatours.notifications;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(String email, Pageable pageable);
    Optional<Notification> findByIdAndRecipientEmailIgnoreCase(Long id, String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select n from Notification n where n.id = :id")
    Optional<Notification> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Notification> findTop20ByEmailSentAtIsNullAndDeliveryAttemptsLessThanAndNextAttemptAtLessThanEqualOrderByIdAsc(
            int maxAttempts, Instant now);
}
