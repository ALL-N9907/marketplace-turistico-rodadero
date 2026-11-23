package com.edu.unimag.rodatours.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** La contraseña sólo se utiliza para autenticar; nunca se devuelve desde la API. */
public record ApiPrincipal(String email, String passwordHash, AccountRole role, Long agencyId,
                           Long touristId, Long guideId, boolean enabled) implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return enabled; }

    @Override
    public String toString() {
        return "ApiPrincipal[email=" + email + ", role=" + role + "]";
    }
}
