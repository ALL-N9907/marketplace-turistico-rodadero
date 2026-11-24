package com.edu.unimag.rodatours.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Una sesión deja de servir al cambiar contraseña, rol, asignación o estado de la cuenta. */
public class SessionRevalidationFilter extends OncePerRequestFilter {
    private final ObjectProvider<UserDetailsService> users;
    public SessionRevalidationFilter(ObjectProvider<UserDetailsService> users) { this.users = users; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof ApiPrincipal cached) {
            boolean valid;
            try {
                var current = users.getObject().loadUserByUsername(cached.email());
                valid = current.isEnabled() && cached.equals(current);
            } catch (UsernameNotFoundException exception) { valid = false; }
            if (!valid) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
            }
        }
        chain.doFilter(request, response);
    }
}
