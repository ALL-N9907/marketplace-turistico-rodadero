package com.edu.unimag.rodatours.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tours", "/api/tours/**", "/api/agencies", "/api/agencies/**",
                                "/api/experience-types", "/api/experience-types/**", "/api/availability/**",
                                "/api/schedules/*", "/api/reviews/*", "/api/catalog/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                // Se conserva CSRF incluso con HTTP Basic: los navegadores pueden reenviar credenciales.
                .csrf(Customizer.withDefaults())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setHeader("WWW-Authenticate", "Basic realm=\"rodatours\"");
                            response.setContentType("application/problem+json");
                            response.getWriter().write("{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"Autenticación requerida.\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/problem+json");
                            response.getWriter().write("{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"No tiene permiso para esta operación.\"}");
                        }))
                .build();
    }
}
