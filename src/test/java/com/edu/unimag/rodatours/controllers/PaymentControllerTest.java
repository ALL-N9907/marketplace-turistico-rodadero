package com.edu.unimag.rodatours.controllers;

import com.edu.unimag.rodatours.api.controllers.PaymentController;
import com.edu.unimag.rodatours.config.SecurityConfig;
import com.edu.unimag.rodatours.payments.*;
import com.edu.unimag.rodatours.security.AccessPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {
    @Autowired MockMvc mvc;@MockitoBean PaymentService service;@MockitoBean PaymentProperties properties;
    @MockitoBean PaymentWebhookService webhooks;@MockitoBean(name="accessPolicy") AccessPolicy access;
    @Test void anonymousCannotStartPayment()throws Exception{mvc.perform(post("/api/reservations/1/payment/checkout").with(csrf())).andExpect(status().isUnauthorized());verifyNoInteractions(service);}
    @Test @WithMockUser(roles="TOURIST") void cannotPayForeignReservation()throws Exception{mvc.perform(post("/api/reservations/1/payment/checkout").with(csrf())).andExpect(status().isForbidden());verifyNoInteractions(service);}
    @Test @WithMockUser(roles="ADMIN") void csrfStillRequiredOutsideWebhook()throws Exception{mvc.perform(post("/api/reservations/1/payment/checkout")).andExpect(status().isForbidden());verifyNoInteractions(service);}
    @Test void onlyWebhookAcceptsSignedProviderRequestsWithoutCsrf()throws Exception{
        mvc.perform(post("/api/payments/webhook").param("data.id","123").header("x-request-id","request").header("x-signature","header"))
                .andExpect(status().isOk());verify(webhooks).accept("123","request","header");
    }
}
