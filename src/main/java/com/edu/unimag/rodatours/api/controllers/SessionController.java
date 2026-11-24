package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.security.ApiPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SessionController {
    private final AuthenticationManager authenticationManager;

    @PostMapping("/api/auth/login")
    public ResponseEntity<AuthController.Identity> login(@Valid @RequestBody Credentials credentials,
            HttpServletRequest request, HttpServletResponse response) {
        var oldSession = request.getSession(false);
        if (oldSession != null) oldSession.invalidate();
        SecurityContextHolder.clearContext();
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(credentials.email().trim(), credentials.password()));
        new ChangeSessionIdAuthenticationStrategy().onAuthentication(authentication, request, response);
        new CsrfAuthenticationStrategy(new HttpSessionCsrfTokenRepository()).onAuthentication(authentication, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        var actor = (ApiPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new AuthController.Identity(
                actor.email(), actor.role(), actor.agencyId(), actor.touristId(), actor.guideId()));
    }

    public record Credentials(@NotBlank @Email @Size(max = 255) String email,
                              @NotBlank @Size(max = 255) String password) {}
}
