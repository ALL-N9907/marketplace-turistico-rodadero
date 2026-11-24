package com.edu.unimag.rodatours.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/** Límite acotado por IP y operación; no confía en cabeceras X-Forwarded-For arbitrarias. */
public class SecurityRateLimitFilter extends OncePerRequestFilter {
    private record Bucket(long expires, int count) {}
    private final Map<String, Bucket> buckets = new HashMap<>();
    private final Clock clock;
    public SecurityRateLimitFilter() { this(Clock.systemUTC()); }
    public SecurityRateLimitFilter(Clock clock) { this.clock = clock; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !java.util.Set.of("/api/auth/login", "/api/auth/register",
                "/api/auth/password-reset/request", "/api/auth/password-reset/confirm").contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = clock.millis();
        boolean reset = request.getServletPath().contains("password-reset");
        long window = reset ? 900_000 : 300_000;
        int max = reset ? 5 : 30;
        String key = request.getRemoteAddr() + ":" + request.getServletPath();
        boolean allowed;
        synchronized (buckets) {
            buckets.entrySet().removeIf(e -> e.getValue().expires() <= now);
            Bucket old = buckets.get(key);
            allowed = old == null ? buckets.size() < 10_000 : old.count() < max;
            if (allowed) buckets.put(key, new Bucket(old == null ? now + window : old.expires(), old == null ? 1 : old.count() + 1));
        }
        if (!allowed) {
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(window / 1000));
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"status\":429,\"detail\":\"Demasiados intentos. Intente más tarde.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
