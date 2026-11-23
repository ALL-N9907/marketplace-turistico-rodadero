package com.edu.unimag.rodatours.security;

import com.edu.unimag.rodatours.domain.repositories.AgencyRepository;
import com.edu.unimag.rodatours.domain.repositories.GuideRepository;
import com.edu.unimag.rodatours.domain.repositories.TouristRepository;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final StaffAccountRepository accounts;
    private final TouristRepository tourists;
    private final AgencyRepository agencies;
    private final GuideRepository guides;
    private final PasswordEncoder encoder;

    @Transactional
    public StaffAccount create(String email, String password, AccountRole role, Long agencyId, Long guideId) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (accounts.existsByEmailIgnoreCase(normalized) || tourists.existsByCorreoElectronicoIgnoreCase(normalized)) {
            throw new ConflictException("El correo ya está registrado.");
        }
        if (role == AccountRole.TOURIST) {
            throw new ValidationException("Registre turistas mediante /api/auth/register.");
        }
        if (role == AccountRole.AGENCY_OPERATOR || role == AccountRole.GUIDE) {
            if (agencyId == null || !agencies.findById(agencyId).map(a -> a.isActive()).orElse(false)) {
                throw new ValidationException("Debe indicar una agencia activa.");
            }
        } else if (agencyId != null || guideId != null) {
            throw new ValidationException("Este rol no admite agencia ni guía asociados.");
        }
        if (role == AccountRole.GUIDE) {
            if (guideId == null || !guides.findById(guideId)
                    .map(g -> g.isActive() && g.getAgencia().getId().equals(agencyId)).orElse(false)) {
                throw new ValidationException("El guía debe estar activo y pertenecer a la agencia.");
            }
        } else if (guideId != null) {
            throw new ValidationException("Sólo una cuenta GUIDE admite guiaId.");
        }
        StaffAccount account = new StaffAccount();
        account.setEmail(normalized);
        account.setPasswordHash(encoder.encode(password));
        account.setRole(role);
        account.setAgencyId(agencyId);
        account.setGuideId(guideId);
        return accounts.save(account);
    }
}
