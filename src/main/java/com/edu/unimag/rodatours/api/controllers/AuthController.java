package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.api.dtos.request.CreateTouristRequest;
import com.edu.unimag.rodatours.api.dtos.response.TouristResponse;
import com.edu.unimag.rodatours.domain.entities.enums.TouristStatus;
import com.edu.unimag.rodatours.security.*;
import com.edu.unimag.rodatours.services.TouristService;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AccountService accounts;
    private final StaffAccountRepository accountRepository;
    private final TouristService tourists;
    private final AccessPolicy access;

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }

    @GetMapping("/me")
    public Identity me() {
        var actor = access.current();
        return new Identity(actor.email(), actor.role(), actor.agencyId(), actor.touristId(), actor.guideId());
    }

    @PostMapping("/register")
    public ResponseEntity<TouristResponse> register(@Valid @RequestBody Registration request) {
        validatePassword(request.password());
        if (accountRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ConflictException("El correo ya está registrado.");
        }
        TouristResponse response = tourists.create(new CreateTouristRequest(request.name(), request.email().trim(),
                request.password(), request.phone(), TouristStatus.ACTIVE));
        return ResponseEntity.created(URI.create("/api/tourists/" + response.id())).body(response);
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Identity> createStaff(@Valid @RequestBody StaffRegistration request) {
        validatePassword(request.password());
        var account = accounts.create(request.email(), request.password(), request.role(), request.agencyId(), request.guideId());
        return ResponseEntity.status(201).body(new Identity(account.getEmail(), account.getRole(),
                account.getAgencyId(), null, account.getGuideId()));
    }

    private void validatePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ValidationException("La contraseña no puede superar 72 bytes UTF-8.");
        }
    }

    public record CsrfResponse(String headerName, String parameterName, String token) {}
    public record Identity(String email, AccountRole role, Long agencyId, Long touristId, Long guideId) {}
    public record Registration(@NotBlank @Size(max = 150) String name, @NotBlank @Email @Size(max = 255) String email,
                               @NotBlank @Size(min = 12, max = 72) String password, @Size(max = 20) String phone) {}
    public record StaffRegistration(@NotBlank @Email @Size(max = 255) String email,
                                    @NotBlank @Size(min = 12, max = 72) String password, @NotNull AccountRole role,
                                    @Positive Long agencyId, @Positive Long guideId) {}
}
