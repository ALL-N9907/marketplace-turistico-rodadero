package com.edu.unimag.rodatours.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import com.edu.unimag.rodatours.security.SessionRevalidationFilter;
import com.edu.unimag.rodatours.security.SecurityRateLimitFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, ObjectProvider<UserDetailsService> users) throws Exception {
        return http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login",
                                "/api/agencies/nearby",
                                "/api/auth/password-reset/request", "/api/auth/password-reset/confirm",
                                "/api/payments/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tours", "/api/tours/**", "/api/agencies", "/api/agencies/**",
                                "/api/experience-types", "/api/experience-types/**", "/api/availability/**",
                                "/api/schedules/*", "/api/reviews/*", "/api/catalog/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .securityContext(context -> context.securityContextRepository(new HttpSessionSecurityContextRepository()))
                .addFilterAfter(new SessionRevalidationFilter(users), SecurityContextHolderFilter.class)
                .addFilterBefore(new SecurityRateLimitFilter(), SecurityContextHolderFilter.class)
                .logout(logout -> logout.logoutUrl("/api/auth/logout").invalidateHttpSession(true)
                        .clearAuthentication(true).deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                // Se conserva CSRF incluso con HTTP Basic: los navegadores pueden reenviar credenciales.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/payments/webhook"))
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
