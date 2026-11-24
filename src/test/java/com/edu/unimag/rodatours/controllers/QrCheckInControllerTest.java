package com.edu.unimag.rodatours.controllers;

import com.edu.unimag.rodatours.api.controllers.QrCheckInController;
import com.edu.unimag.rodatours.config.SecurityConfig;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.services.QrCheckInService;
import com.edu.unimag.rodatours.services.ReservationVoucherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QrCheckInController.class)
@Import(SecurityConfig.class)
class QrCheckInControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean QrCheckInService service;
    private static final String BODY = "{\"qrPayload\":\"rodatours:reservation:unique-code\"}";

    @Test
    @WithMockUser(roles = "GUIDE")
    void acceptsPayloadWithoutIdAndReturnsPrivateResponse() throws Exception {
        when(service.checkIn("rodatours:reservation:unique-code")).thenReturn(
                new ReservationVoucherService.Voucher(1L, "Minca", "Agencia", null, null, 2, null,
                        ReservationStatus.CONFIRMED, "rodatours:reservation:unique-code", null));
        mvc.perform(post("/api/reservations/check-in").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.reservationId").value(1));
    }

    @Test
    @WithMockUser(roles = "TOURIST")
    void rejectsTourist() throws Exception {
        mvc.perform(post("/api/reservations/check-in").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void csrfAndNonBlankPayloadAreRequired() throws Exception {
        mvc.perform(post("/api/reservations/check-in").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/reservations/check-in").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"qrPayload\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
