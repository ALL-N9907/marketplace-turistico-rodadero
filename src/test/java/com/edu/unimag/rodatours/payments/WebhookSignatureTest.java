package com.edu.unimag.rodatours.payments;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

class WebhookSignatureTest {
    static String signature(String id,String request,String ts,String secret)throws Exception{
        var m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        return "ts="+ts+",v1="+HexFormat.of().formatHex(m.doFinal(("id:"+id+";request-id:"+request+";ts:"+ts+";").getBytes(StandardCharsets.UTF_8)));
    }
    @Test void acceptsCorrectHmacAndRejectsManipulatedResource()throws Exception{
        var properties=PaymentServiceTest.configured();var verifier=new WebhookSignature(properties);String ts=Long.toString(Instant.now().getEpochSecond());
        String header=signature("123","request-1",ts,properties.getWebhookSecret());
        assertTrue(verifier.verify("123","request-1",header).fresh());
        assertThrows(BadCredentialsException.class,()->verifier.verify("124","request-1",header));
        assertThrows(BadCredentialsException.class,()->verifier.verify("123","request-2",header));
    }
    @Test void oldValidSignatureIsRecognizedButNotFresh()throws Exception{
        var p=PaymentServiceTest.configured();String ts=Long.toString(Instant.now().minusSeconds(3600).getEpochSecond());
        assertFalse(new WebhookSignature(p).verify("123","request-1",signature("123","request-1",ts,p.getWebhookSecret())).fresh());
    }
    @Test void malformedOrRepeatedHeaderFieldsAreRejected(){var verifier=new WebhookSignature(PaymentServiceTest.configured());assertThrows(BadCredentialsException.class,()->verifier.verify("123","request-1","ts=1,ts=2,v1=bad"));assertThrows(BadCredentialsException.class,()->verifier.verify("123","request-1",null));}
}
