package com.edu.unimag.rodatours.security;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface StaffAccountRepository extends JpaRepository<StaffAccount, Long> {
    Optional<StaffAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<StaffAccount> findByAgencyIdAndRoleAndEnabledTrue(Long agencyId, AccountRole role);
}
