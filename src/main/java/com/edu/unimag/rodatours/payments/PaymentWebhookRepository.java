package com.edu.unimag.rodatours.payments;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.*;

public interface PaymentWebhookRepository extends JpaRepository<PaymentWebhook, Long> {
    boolean existsByEventKey(String key);
    List<PaymentWebhook> findTop10ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(String status, Instant now);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from PaymentWebhook w where w.id = :id")
    Optional<PaymentWebhook> lockById(@Param("id") Long id);
    @Modifying
    @Query(value = """
            insert into payment_webhooks(event_key,provider_payment_id,status,attempts,created_at,next_attempt_at,version)
            values (:key,:paymentId,'PENDING',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0)
            on conflict (event_key) do nothing
            """, nativeQuery = true)
    void insertIfAbsent(@Param("key") String key, @Param("paymentId") String paymentId);
    Page<PaymentWebhook> findAllByOrderByIdDesc(Pageable pageable);
}
