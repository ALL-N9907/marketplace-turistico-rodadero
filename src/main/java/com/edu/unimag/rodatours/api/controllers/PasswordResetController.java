package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.security.reset.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {
    private final PasswordResetService resets;
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> request(@Valid @RequestBody Request body) {
        resets.request(body.email());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(Map.of("message",
                "Si existe una cuenta habilitada para ese correo, recibirá instrucciones de recuperación."));
    }
    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@Valid @RequestBody Confirm body, jakarta.servlet.http.HttpServletRequest request) {
        resets.confirm(body.token(), body.password());
        var session=request.getSession(false);if(session!=null)session.invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    public record Request(@NotBlank @Email @Size(max = 255) String email) {}
    public record Confirm(@NotBlank @Size(max = 100) String token, @NotBlank @Size(min = 12, max = 72) String password) {}
}
