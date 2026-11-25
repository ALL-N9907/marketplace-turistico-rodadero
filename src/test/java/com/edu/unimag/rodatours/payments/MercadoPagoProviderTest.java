package com.edu.unimag.rodatours.payments;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MercadoPagoProviderTest {
    private static final String LIVE_PAYMENT = """
            {"id":1,"external_reference":"ref","transaction_amount":100000,"currency_id":"COP",
             "status":"approved","live_mode":true,"collector_id":123,"payer":{"id":456},
             "transaction_amount_refunded":0,"date_last_updated":"2026-11-25T00:00:00Z"}
            """;
    @Test void liveFlagRequiresExplicitBuyerAndVerifiedTestSellerAndBuyerProfile(){
        var builder=RestClient.builder().baseUrl("https://api.mercadopago.com");var server=MockRestServiceServer.bindTo(builder).build();
        var p=PaymentServiceTest.configured();p.setAccessToken("APP_USR-test-account");p.setTestBuyerId("456");
        server.expect(requestTo("https://api.mercadopago.com/users/me"))
                .andRespond(withSuccess("{\"id\":123,\"tags\":[\"test_user\"]}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/1")).andRespond(withSuccess(LIVE_PAYMENT,MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/users/456"))
                .andRespond(withSuccess("{\"id\":456,\"nickname\":\"TESTUSER456\"}",MediaType.APPLICATION_JSON));
        var remote=new MercadoPagoProvider(p,builder.build()).get("1");
        assertTrue(remote.liveMode());assertEquals("456",remote.verifiedTestBuyerId());server.verify();
    }
    @ParameterizedTest
    @ValueSource(strings={"", "789", "123", "not-an-id"})
    void liveFlagIsRejectedWithoutMatchingExplicitDistinctBuyer(String buyer){
        var builder=RestClient.builder().baseUrl("https://api.mercadopago.com");var server=MockRestServiceServer.bindTo(builder).build();
        var p=PaymentServiceTest.configured();p.setAccessToken("APP_USR-test-account");p.setTestBuyerId(buyer);
        server.expect(requestTo("https://api.mercadopago.com/users/me"))
                .andRespond(withSuccess("{\"id\":123,\"tags\":[\"test_user\"]}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/1")).andRespond(withSuccess(LIVE_PAYMENT,MediaType.APPLICATION_JSON));
        assertThrows(PaymentProviderException.class,()->new MercadoPagoProvider(p,builder.build()).get("1"));server.verify();
    }
    @ParameterizedTest
    @ValueSource(strings={"{}", "{\"id\":456,\"nickname\":\"REALUSER\"}", "{\"id\":789,\"nickname\":\"TESTUSER456\"}"})
    void buyerProfileMustMatchAndHaveTestFormat(String account){
        var builder=RestClient.builder().baseUrl("https://api.mercadopago.com");var server=MockRestServiceServer.bindTo(builder).build();
        var p=PaymentServiceTest.configured();p.setAccessToken("APP_USR-test-account");p.setTestBuyerId("456");
        server.expect(requestTo("https://api.mercadopago.com/users/me"))
                .andRespond(withSuccess("{\"id\":123,\"tags\":[\"test_user\"]}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/1")).andRespond(withSuccess(LIVE_PAYMENT,MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/users/456")).andRespond(withSuccess(account,MediaType.APPLICATION_JSON));
        assertThrows(PaymentProviderException.class,()->new MercadoPagoProvider(p,builder.build()).get("1"));server.verify();
    }
    @Test void testTokenPrefixAloneCannotOverrideLiveFlag(){
        var builder=RestClient.builder().baseUrl("https://api.mercadopago.com");var server=MockRestServiceServer.bindTo(builder).build();
        var p=PaymentServiceTest.configured();p.setTestBuyerId("456");
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/1")).andRespond(withSuccess(LIVE_PAYMENT,MediaType.APPLICATION_JSON));
        assertThrows(PaymentProviderException.class,()->new MercadoPagoProvider(p,builder.build()).get("1"));server.verify();
    }
    @Test void acceptsAppUsrOnlyAfterVerifyingTestSeller(){
        var builder=RestClient.builder().baseUrl("https://api.mercadopago.com");var server=MockRestServiceServer.bindTo(builder).build();
        var p=PaymentServiceTest.configured();p.setAccessToken("APP_USR-test-account");
        server.expect(requestTo("https://api.mercadopago.com/users/me"))
                .andExpect(header("Authorization","Bearer APP_USR-test-account"))
                .andRespond(withSuccess("{\"id\":123,\"tags\":[\"test_user\"]}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/checkout/preferences"))
                .andRespond(withSuccess("{\"id\":\"pref-1\",\"sandbox_init_point\":\"https://sandbox.mercadopago.com.co/checkout/v1/redirect?pref_id=1\"}",MediaType.APPLICATION_JSON));
        assertEquals("pref-1",new MercadoPagoProvider(p,builder.build()).create("ref",7L,BigDecimal.TEN).id());server.verify();
    }
    @ParameterizedTest
    @ValueSource(strings={"{\"id\":123,\"tags\":[]}","{\"id\":456,\"tags\":[\"test_user\"]}","{\"id\":123}","null"})
    void rejectsRealWrongOrUnverifiableAccountsBeforeAnyPaymentRequest(String account){
        for(int operation=0;operation<3;operation++){
            var builder=RestClient.builder().baseUrl("https://api.mercadopago.com");var server=MockRestServiceServer.bindTo(builder).build();
            var p=PaymentServiceTest.configured();p.setAccessToken("APP_USR-test-account");
            server.expect(requestTo("https://api.mercadopago.com/users/me")).andRespond(withSuccess(account,MediaType.APPLICATION_JSON));
            var provider=new MercadoPagoProvider(p,builder.build());
            int selected=operation;
            assertThrows(PaymentProviderException.class,()->{
                switch(selected){case 0 -> provider.create("ref",7L,BigDecimal.TEN);case 1 -> provider.get("1");default -> provider.search("ref");}
            });server.verify();
        }
    }
    @Test void accountLookupFailureBlocksCheckoutWithoutLeakingCredentials(){
        var builder=RestClient.builder().baseUrl("https://api.mercadopago.com");var server=MockRestServiceServer.bindTo(builder).build();
        var p=PaymentServiceTest.configured();p.setAccessToken("APP_USR-test-account");
        server.expect(requestTo("https://api.mercadopago.com/users/me")).andRespond(withServerError());
        var error=assertThrows(PaymentProviderException.class,()->new MercadoPagoProvider(p,builder.build()).create("ref",7L,BigDecimal.TEN));
        assertFalse(error.getMessage().contains(p.getAccessToken()));server.verify();
    }
    @Test void verifiedTestAccountStillCannotReturnProductiveCheckout(){
        var builder=RestClient.builder().baseUrl("https://api.mercadopago.com");var server=MockRestServiceServer.bindTo(builder).build();
        var p=PaymentServiceTest.configured();p.setAccessToken("APP_USR-test-account");
        server.expect(requestTo("https://api.mercadopago.com/users/me"))
                .andRespond(withSuccess("{\"id\":123,\"tags\":[\"test_user\"]}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/checkout/preferences"))
                .andRespond(withSuccess("{\"id\":\"pref-1\",\"sandbox_init_point\":\"https://www.mercadopago.com.co/checkout\"}",MediaType.APPLICATION_JSON));
        assertThrows(PaymentProviderException.class,()->new MercadoPagoProvider(p,builder.build()).create("ref",7L,BigDecimal.TEN));server.verify();
    }
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
