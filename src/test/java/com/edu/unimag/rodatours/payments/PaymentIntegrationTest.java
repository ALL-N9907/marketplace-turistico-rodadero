package com.edu.unimag.rodatours.payments;

import com.edu.unimag.rodatours.repositories.TestPostgresContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop","rodatours.payments.enabled=true",
        "rodatours.payments.access-token=TEST-only-tests","rodatours.payments.webhook-secret=test-secret-at-least-16",
        "rodatours.payments.collector-id=123","rodatours.payments.frontend-url=https://example.com",
        "rodatours.payments.notification-url=https://example.com/api/payments/webhook"})
@Transactional
class PaymentIntegrationTest {
    @DynamicPropertySource static void postgres(DynamicPropertyRegistry r){TestPostgresContainer.register(r);}
    @Autowired PaymentWebhookService inbox;@Autowired PaymentWebhookRepository events;
    @Autowired PaymentRepository payments;@Autowired PaymentWebhookProcessor processor;@Autowired PaymentProperties properties;
    @MockitoBean PaymentProvider provider;
    @Test void duplicateSignedWebhookIsPersistedOnlyOnceAndVerifiedPaymentIsApplied()throws Exception{
        String ts=Long.toString(Instant.now().getEpochSecond());String signature=WebhookSignatureTest.signature("123","request-1",ts,properties.getWebhookSecret());
        inbox.accept("123","request-1",signature);inbox.accept("123","request-1",signature);
        assertEquals(1,events.count());
        var payment=new Payment();payment.setReservationId(123L);payment.setReference("test-ref");payment.setAmount(new BigDecimal("180000"));payments.saveAndFlush(payment);
        when(provider.get("123")).thenReturn(new PaymentProvider.RemotePayment("123","test-ref",new BigDecimal("180000"),"COP","approved",false,"123",BigDecimal.ZERO,Instant.now()));
        var event=events.findAll().getFirst();processor.process(event.getId());
        assertEquals("DONE",event.getStatus());assertEquals("APPROVED",payments.findByReservationId(123L).orElseThrow().getStatus());
        processor.process(event.getId());verify(provider,times(1)).get("123");
    }
}
