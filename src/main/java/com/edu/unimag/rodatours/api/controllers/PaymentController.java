package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.payments.*;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService payments;
    private final PaymentProperties properties;
    private final PaymentWebhookService webhooks;

    @GetMapping("/api/payments/config")
    public Map<String, Object> config() {
        boolean ready;
        try { properties.requireConfigured(); ready = true; } catch (RuntimeException e) { ready = false; }
        return Map.of("enabled", ready, "sandbox", true);
    }
    @GetMapping("/api/reservations/{id}/payment")
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.readReservation(#id)")
    public ResponseEntity<PaymentService.PaymentView> get(@PathVariable @Positive Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(payments.get(id));
    }
    @PostMapping("/api/reservations/{id}/payment/checkout")
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.touristReservation(#id)")
    public ResponseEntity<PaymentService.PaymentView> checkout(@PathVariable @Positive Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(payments.checkout(id));
    }
    @PostMapping("/api/reservations/{id}/payment/reconcile")
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.touristReservation(#id) or @accessPolicy.operateReservation(#id)")
    public ResponseEntity<PaymentService.PaymentView> reconcile(@PathVariable @Positive Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(payments.reconcile(id));
    }
    @PostMapping("/api/payments/webhook")
    public ResponseEntity<Void> webhook(@RequestParam("data.id") String id,
            @RequestHeader("x-request-id") String requestId, @RequestHeader("x-signature") String signature) {
        // El cuerpo no se usa como autoridad; sólo se encola el ID firmado para consultar al proveedor.
        webhooks.accept(id, requestId, signature);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).build();
    }
}
