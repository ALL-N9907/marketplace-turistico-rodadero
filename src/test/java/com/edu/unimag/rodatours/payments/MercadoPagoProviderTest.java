package com.edu.unimag.rodatours.payments;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MercadoPagoProviderTest {
    @Test void createsHostedSandboxCheckoutAndNeverUsesProductiveRedirect(){
        var builder=RestClient.builder().baseUrl("https://api.mercadopago.com");var server=MockRestServiceServer.bindTo(builder).build();var p=PaymentServiceTest.configured();
        var provider=new MercadoPagoProvider(p,builder.build());
        server.expect(requestTo("https://api.mercadopago.com/checkout/preferences"))
                .andExpect(header("Authorization","Bearer TEST-token"))
                .andExpect(jsonPath("$.items[0].unit_price").value(180000))
                .andExpect(jsonPath("$.external_reference").value("ref"))
                .andRespond(withSuccess("{\"id\":\"pref-1\",\"sandbox_init_point\":\"https://sandbox.mercadopago.com.co/checkout/v1/redirect?pref_id=1\"}",MediaType.APPLICATION_JSON));
        assertEquals("pref-1",provider.create("ref",7L,new BigDecimal("180000")).id());server.verify();
    }
    @Test void providerFailureDoesNotExposeResponseOrToken(){
        var builder=RestClient.builder().baseUrl("https://api.mercadopago.com");var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/1")).andRespond(withServerError());
        var error=assertThrows(PaymentProviderException.class,()->new MercadoPagoProvider(PaymentServiceTest.configured(),builder.build()).get("1"));
        assertFalse(error.getMessage().contains("TEST-token"));server.verify();
    }
}
