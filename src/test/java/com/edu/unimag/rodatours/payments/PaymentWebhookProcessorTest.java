package com.edu.unimag.rodatours.payments;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentWebhookProcessorTest {
    @Test void newEventCompletesOnceAndDoneEventDoesNotReapplyPayment(){
        var inbox=mock(PaymentWebhookRepository.class);var provider=mock(PaymentProvider.class);var payments=mock(PaymentService.class);
        var processor=new PaymentWebhookProcessor(inbox,provider,payments);
        var event=new PaymentWebhook();event.setId(1L);event.setProviderPaymentId("123");event.setNextAttemptAt(Instant.now().minusSeconds(1));
        var remote=new PaymentProvider.RemotePayment("123","ref",BigDecimal.TEN,"COP","approved",false,"456",BigDecimal.ZERO,Instant.now());
        when(inbox.lockById(1L)).thenReturn(Optional.of(event));when(provider.get("123")).thenReturn(remote);
        processor.process(1L);processor.process(1L);
        assertEquals("DONE",event.getStatus());assertEquals("VERIFIED",event.getOutcome());assertEquals(1,event.getAttempts());
        verify(provider,times(1)).get("123");verify(payments,times(1)).apply(remote);verify(inbox,times(1)).save(event);
    }
    @Test void providerFailureNeverMarksEventDone(){
        var inbox=mock(PaymentWebhookRepository.class);var provider=mock(PaymentProvider.class);var payments=mock(PaymentService.class);
        var event=new PaymentWebhook();event.setId(1L);event.setProviderPaymentId("123");event.setNextAttemptAt(Instant.now().minusSeconds(1));
        when(inbox.lockById(1L)).thenReturn(Optional.of(event));when(provider.get("123")).thenThrow(new PaymentProviderException());
        var processor=new PaymentWebhookProcessor(inbox,provider,payments);
        assertThrows(PaymentProviderException.class,()->processor.process(1L));assertEquals("PENDING",event.getStatus());
        verifyNoInteractions(payments);verify(inbox,never()).save(any());
    }
}
