package com.edu.unimag.rodatours.payments;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Component
public class MercadoPagoProvider implements PaymentProvider {
    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {};
    private final PaymentProperties properties;
    private final RestClient client;
    @Autowired
    public MercadoPagoProvider(PaymentProperties properties) { this(properties, client()); }
    MercadoPagoProvider(PaymentProperties properties, RestClient client) { this.properties = properties; this.client = client; }
    private static RestClient client() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().baseUrl("https://api.mercadopago.com").requestFactory(factory).build();
    }
    private boolean requireTestAccount() {
        // APP_USR también se usa en cuentas de prueba: el prefijo no acredita el entorno.
        // Se verifica antes de cualquier operación; ante error o metadatos ausentes se bloquea.
        if (!properties.getAccessToken().startsWith("APP_USR-")) return false;
        var account = client.get().uri("/users/me").headers(h -> h.setBearerAuth(properties.getAccessToken()))
                .retrieve().body(MAP);
        if (account == null || !properties.getCollectorId().equals(String.valueOf(account.get("id")))
                || !(account.get("tags") instanceof List<?> tags) || !tags.contains("test_user"))
            throw new PaymentProviderException();
        return true;
    }
    private String verifyTestBuyer(Map<String, Object> payment, boolean verifiedSeller) {
        String expected = properties.getTestBuyerId();
        if (!verifiedSeller || expected == null || !expected.matches("[0-9]+")
                || expected.equals(properties.getCollectorId())
                || !properties.getCollectorId().equals(String.valueOf(payment.get("collector_id")))
                || !(payment.get("payer") instanceof Map<?, ?> payer)
                || !expected.equals(String.valueOf(payer.get("id")))) throw new PaymentProviderException();
        // El ID se habilita explícitamente desde Cuentas de prueba, no desde el navegador.
        // La consulta pública no expone tags: el nombre TESTUSER es sólo una defensa adicional,
        // no sustituye al vendedor test_user ni a la lista explícita de compradores permitidos.
        var buyer = client.get().uri("/users/{id}", expected).headers(h -> h.setBearerAuth(properties.getAccessToken()))
                .retrieve().body(MAP);
        if (buyer == null || !expected.equals(String.valueOf(buyer.get("id")))
                || !(buyer.get("nickname") instanceof String nickname) || !nickname.matches("TESTUSER[0-9]+"))
            throw new PaymentProviderException();
        return expected;
    }
    @Override
    public Checkout create(String reference, Long reservationId, BigDecimal amount) {
        properties.requireConfigured();
        String back = properties.getFrontendUrl().replaceAll("/+$", "") + "/pagos?reservaId=" + reservationId;
        try {
            requireTestAccount();
            var body = Map.of("items", List.of(Map.of("title", "Reserva Rodatours RT-" + reservationId,
                            "quantity", 1, "currency_id", "COP", "unit_price", amount)),
                    "external_reference", reference, "notification_url", properties.getNotificationUrl(),
                    "back_urls", Map.of("success", back, "failure", back, "pending", back),
                    "expires", true, "expiration_date_to", OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30).toString());
            var data = client.post().uri("/checkout/preferences").headers(h -> h.setBearerAuth(properties.getAccessToken()))
                    .header("X-Idempotency-Key", reference).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(MAP);
            if (data == null || data.get("id") == null || !PaymentProperties.sandboxUrl(String.valueOf(data.get("sandbox_init_point")))) throw new PaymentProviderException();
            return new Checkout(String.valueOf(data.get("id")), String.valueOf(data.get("sandbox_init_point")));
        } catch (RuntimeException e) { throw new PaymentProviderException(); }
    }
    @Override
    public RemotePayment get(String id) {
        properties.requireConfigured();
        if (id == null || !id.matches("[0-9]{1,30}")) throw new PaymentProviderException();
        try {
            boolean verifiedSeller = requireTestAccount();
            var data = client.get().uri("/v1/payments/{id}", id).headers(h -> h.setBearerAuth(properties.getAccessToken())).retrieve().body(MAP);
            if (data == null || !id.equals(String.valueOf(data.get("id"))) || !(data.get("live_mode") instanceof Boolean live)) throw new PaymentProviderException();
            return new RemotePayment(id, String.valueOf(data.get("external_reference")), new BigDecimal(String.valueOf(data.get("transaction_amount"))),
                    String.valueOf(data.get("currency_id")), String.valueOf(data.get("status")), live, String.valueOf(data.get("collector_id")),
                    new BigDecimal(String.valueOf(data.getOrDefault("transaction_amount_refunded", 0))),
                    OffsetDateTime.parse(String.valueOf(data.get("date_last_updated"))).toInstant(),
                    live ? verifyTestBuyer(data, verifiedSeller) : null);
        } catch (RuntimeException e) { throw new PaymentProviderException(); }
    }
    @Override
    public List<String> search(String reference) {
        properties.requireConfigured();
        try {
            requireTestAccount();
            var data = client.get().uri(b -> b.path("/v1/payments/search").queryParam("external_reference", reference)
                            .queryParam("sort", "date_created").queryParam("criteria", "desc").queryParam("limit", 20).build())
                    .headers(h -> h.setBearerAuth(properties.getAccessToken())).retrieve().body(MAP);
            if (data == null || !(data.get("results") instanceof List<?> results)) throw new PaymentProviderException();
            return results.stream().map(r -> (Map<?, ?>) r).map(r -> String.valueOf(r.get("id"))).toList();
        } catch (RuntimeException e) { throw new PaymentProviderException(); }
    }
}
