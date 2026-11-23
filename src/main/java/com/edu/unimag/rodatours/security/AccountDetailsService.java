package com.edu.unimag.rodatours.security;

import com.edu.unimag.rodatours.domain.repositories.AgencyRepository;
import com.edu.unimag.rodatours.domain.repositories.GuideRepository;
import com.edu.unimag.rodatours.domain.repositories.TouristRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountDetailsService implements UserDetailsService {
    private final StaffAccountRepository accounts;
    private final TouristRepository tourists;
    private final AgencyRepository agencies;
    private final GuideRepository guides;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        String email = username.trim();
        var staff = accounts.findByEmailIgnoreCase(email);
        if (staff.isPresent()) {
            StaffAccount account = staff.get();
            boolean enabled = account.isEnabled();
            if (account.getAgencyId() != null) {
                enabled &= agencies.findById(account.getAgencyId()).map(a -> a.isActive()).orElse(false);
            }
            if (account.getGuideId() != null) {
                enabled &= guides.findById(account.getGuideId())
                        .map(g -> g.isActive() && g.getAgencia().getId().equals(account.getAgencyId())).orElse(false);
            }
            return new ApiPrincipal(account.getEmail(), account.getPasswordHash(), account.getRole(),
                    account.getAgencyId(), null, account.getGuideId(), enabled);
        }
        return tourists.findByCorreoElectronicoIgnoreCase(email)
                .map(t -> new ApiPrincipal(t.getCorreoElectronico(), t.getContrasena(), AccountRole.TOURIST,
                        null, t.getId(), null, t.isActive()))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas."));
    }
}
