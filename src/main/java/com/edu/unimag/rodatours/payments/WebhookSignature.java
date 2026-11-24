package com.edu.unimag.rodatours.payments;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.BadCredentialsException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
public class WebhookSignature {
    private final PaymentProperties properties;
    public Verified verify(String paymentId, String requestId, String header) {
        properties.requireConfigured();
        try {
            if (!paymentId.matches("[0-9]{1,30}") || !requestId.matches("[A-Za-z0-9-]{1,200}") || header.length() > 500) throw new IllegalArgumentException();
            Map<String,String> parts = new HashMap<>();
            for (String segment : header.split(",")) { String[] kv = segment.trim().split("=", 2); if (kv.length != 2 || parts.put(kv[0], kv[1]) != null) throw new IllegalArgumentException(); }
            String ts = parts.get("ts"), signature = parts.get("v1");
            if (ts == null || !ts.matches("[0-9]{10,13}") || signature == null || !signature.matches("[a-fA-F0-9]{64}")) throw new IllegalArgumentException();
            String manifest = "id:" + paymentId + ";request-id:" + requestId + ";ts:" + ts + ";";
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            if (!MessageDigest.isEqual(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)), HexFormat.of().parseHex(signature))) throw new IllegalArgumentException();
            String key = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(manifest.getBytes(StandardCharsets.UTF_8)));
            long value = Long.parseLong(ts);
            Instant issuedAt = ts.length() > 10 ? Instant.ofEpochMilli(value) : Instant.ofEpochSecond(value);
            return new Verified(key, Math.abs(Instant.now().getEpochSecond() - issuedAt.getEpochSecond()) <= 600);
        } catch (Exception e) { throw new BadCredentialsException("Firma de notificación inválida."); }
    }
    public record Verified(String key, boolean fresh) {}
}
