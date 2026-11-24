package com.edu.unimag.rodatours.security;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface StaffAccountRepository extends JpaRepository<StaffAccount, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from StaffAccount a where a.id = :id")
    Optional<StaffAccount> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<StaffAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<StaffAccount> findByAgencyIdAndRoleAndEnabledTrue(Long agencyId, AccountRole role);
}
