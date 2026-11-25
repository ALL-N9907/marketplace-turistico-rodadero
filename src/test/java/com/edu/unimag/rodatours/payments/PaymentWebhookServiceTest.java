package com.edu.unimag.rodatours.payments;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentWebhookServiceTest {
    @Test void invalidSignatureNeverTouchesInbox(){
        var signatures=new WebhookSignature(PaymentServiceTest.configured());var repo=mock(PaymentWebhookRepository.class);
        var service=new PaymentWebhookService(signatures,repo);
        String ts=Long.toString(java.time.Instant.now().getEpochSecond());
        assertThrows(BadCredentialsException.class,()->service.accept("123","invalid-request","ts="+ts+",v1="+"0".repeat(64)));
        verifyNoInteractions(repo);
    }
    @Test void duplicatesUseDatabaseUpsertAndExpiredNewEventsAreRejected(){
        var signatures=mock(WebhookSignature.class);var repo=mock(PaymentWebhookRepository.class);var service=new PaymentWebhookService(signatures,repo);
        when(signatures.verify("1","request","header")).thenReturn(new WebhookSignature.Verified("key",true));
        service.accept("1","request","header");service.accept("1","request","header");verify(repo,times(2)).insertIfAbsent("key","1");
        when(signatures.verify("1","request","header")).thenReturn(new WebhookSignature.Verified("key",false));
        assertThrows(BadCredentialsException.class,()->service.accept("1","request","header"));
        when(repo.existsByEventKey("key")).thenReturn(true);assertDoesNotThrow(()->service.accept("1","request","header"));
    }
}
