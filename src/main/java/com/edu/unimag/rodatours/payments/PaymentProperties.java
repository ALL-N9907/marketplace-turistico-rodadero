package com.edu.unimag.rodatours.payments;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;

@Component
@ConfigurationProperties(prefix = "rodatours.payments")
@Getter @Setter
public class PaymentProperties {
    private boolean enabled;
    private String accessToken = "";
    private String webhookSecret = "";
    private String collectorId = "";
    // Opt-in local para el flujo de dos cuentas de prueba que devuelve live_mode=true.
    private String testBuyerId = "";
    private String frontendUrl = "";
    private String notificationUrl = "";

    public void requireConfigured() {
        if (!enabled || accessToken == null || !accessToken.matches("(?:TEST-|APP_USR-)\\S+") || webhookSecret.length() < 16 || !collectorId.matches("[0-9]+")
                || !secureUrl(frontendUrl) || !secureUrl(notificationUrl))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Mercado Pago sandbox no está configurado. Sólo se admiten cuentas de prueba.");
    }
    private boolean secureUrl(String value) {
        try { var u = URI.create(value); return "https".equals(u.getScheme()) && u.getHost() != null
                && u.getUserInfo() == null && u.getRawQuery() == null && u.getRawFragment() == null; }
        catch (Exception e) { return false; }
    }
    public static boolean sandboxUrl(String value) {
        try { var u = URI.create(value); return "https".equals(u.getScheme()) && u.getUserInfo() == null
                && java.util.Set.of("sandbox.mercadopago.com.co", "sandbox.mercadopago.com").contains(u.getHost()); }
        catch (Exception e) { return false; }
    }
}
