package com.edu.unimag.rodatours.config;

import com.edu.unimag.rodatours.security.AccountRole;
import com.edu.unimag.rodatours.security.AccountService;
import com.edu.unimag.rodatours.security.StaffAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Alta inicial explícita, sin credenciales predeterminadas ni restablecer contraseñas existentes. */
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
    private final AccountService accounts;
    private final StaffAccountRepository repository;
    @Value("${rodatours.bootstrap.admin-email:}") private String email;
    @Value("${rodatours.bootstrap.admin-password:}") private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        if (email.isBlank() || password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Configure ADMIN_EMAIL y ADMIN_PASSWORD (12 caracteres mínimo, 72 bytes máximo).");
        }
        if (!repository.existsByEmailIgnoreCase(email.trim())) {
            accounts.create(email, password, AccountRole.ADMIN, null, null);
        }
    }
}
