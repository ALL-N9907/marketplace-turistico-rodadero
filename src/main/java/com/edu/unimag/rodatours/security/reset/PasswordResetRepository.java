package com.edu.unimag.rodatours.security.reset;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import java.util.*;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findByEmail(String email);
    Optional<PasswordReset> findByTokenHash(String hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PasswordReset p where p.tokenHash = :hash")
    Optional<PasswordReset> lockByTokenHash(@org.springframework.data.repository.query.Param("hash") String hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PasswordReset> findTop10BySentAtIsNullAndConsumedAtIsNullAndEncryptedTokenIsNotNullAndAttemptsLessThanAndNextAttemptAtLessThanEqualOrderByIdAsc(int attempts, Instant now);
}
