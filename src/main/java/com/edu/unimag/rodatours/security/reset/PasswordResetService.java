package com.edu.unimag.rodatours.security.reset;

import com.edu.unimag.rodatours.domain.repositories.TouristRepository;
import com.edu.unimag.rodatours.security.*;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final PasswordResetProperties properties;
    private final ResetTokenCodec codec;
    private final PasswordResetRepository resets;
    private final AccountDetailsService users;
    private final StaffAccountRepository staff;
    private final TouristRepository tourists;
    private final PasswordEncoder encoder;
    @Value("${rodatours.notifications.mail-enabled:false}") private boolean mailEnabled;

    private void configured() {
        try {
            URI uri = URI.create(properties.getFrontendUrl());
            boolean loopback = "http".equals(uri.getScheme()) && java.util.Set.of("localhost", "127.0.0.1").contains(uri.getHost());
            if (!properties.isEnabled() || !mailEnabled || (!"https".equals(uri.getScheme()) && !loopback)
                    || uri.getHost() == null || uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getUserInfo() != null) throw new IllegalArgumentException();
            codec.key();
        } catch (Exception e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "La recuperación de cuentas no está configurada."); }
    }

    @Transactional
    public void request(String email) {
        configured();
        ApiPrincipal actor;
        try { actor = (ApiPrincipal) users.loadUserByUsername(email.trim()); }
        catch (UsernameNotFoundException e) { codec.hash(codec.generate()); return; }
        if (!actor.isEnabled()) return;
        lockAccount(actor.email());
        String normalized = actor.email().toLowerCase(Locale.ROOT);
        var reset = resets.findByEmail(normalized).orElseGet(PasswordReset::new);
        Instant now = Instant.now();
        if (reset.getRequestedAt() != null && reset.getRequestedAt().plusSeconds(60).isAfter(now)) return;
        String token = codec.generate();
        reset.setEmail(normalized); reset.setTokenHash(codec.hash(token)); reset.setEncryptedToken(codec.encrypt(token));
        reset.setRequestedAt(now); reset.setExpiresAt(now.plusSeconds(1800)); reset.setConsumedAt(null);
        reset.setSentAt(null); reset.setAttempts(0); reset.setNextAttemptAt(now);
        resets.save(reset);
    }

    @Transactional
    public void confirm(String token, String password) {
        configured();
        if (password == null || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ValidationException("Use al menos 12 caracteres y como máximo 72 bytes UTF-8.");
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalid();
        String hash = codec.hash(token);
        var candidate = resets.findByTokenHash(hash).orElseThrow(PasswordResetService::invalid);
        lockAccount(candidate.getEmail());
        var reset = resets.lockByTokenHash(hash).orElseThrow(PasswordResetService::invalid);
        if (reset.getConsumedAt() != null || !reset.getExpiresAt().isAfter(Instant.now())) throw invalid();
        String encoded = encoder.encode(password);
        var account = staff.findByEmailIgnoreCase(reset.getEmail());
        if (account.isPresent()) { account.get().setPasswordHash(encoded); staff.save(account.get()); }
        else {
            var tourist = tourists.findByCorreoElectronicoIgnoreCase(reset.getEmail()).orElseThrow(PasswordResetService::invalid);
            tourist.setContrasena(encoded); tourists.save(tourist);
        }
        reset.setConsumedAt(Instant.now()); reset.setEncryptedToken(null); resets.save(reset);
    }

    private void lockAccount(String email) {
        var account = staff.findByEmailIgnoreCase(email);
        if (account.isPresent()) staff.lockById(account.get().getId()).orElseThrow(PasswordResetService::invalid);
        else {
            var tourist = tourists.findByCorreoElectronicoIgnoreCase(email).orElseThrow(PasswordResetService::invalid);
            tourists.findByIdForUpdate(tourist.getId()).orElseThrow(PasswordResetService::invalid);
        }
    }
    private static ValidationException invalid() { return new ValidationException("El enlace es inválido, ya se utilizó o ha vencido."); }
}
