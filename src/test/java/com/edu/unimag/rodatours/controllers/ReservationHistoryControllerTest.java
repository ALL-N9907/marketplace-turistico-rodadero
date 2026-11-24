package com.edu.unimag.rodatours.controllers;

import com.edu.unimag.rodatours.api.controllers.ReservationHistoryController;
import com.edu.unimag.rodatours.config.SecurityConfig;
import com.edu.unimag.rodatours.history.ReservationHistoryService;
import com.edu.unimag.rodatours.security.AccessPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationHistoryController.class)
@Import(SecurityConfig.class)
class ReservationHistoryControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ReservationHistoryService service;
    @MockitoBean(name = "accessPolicy") AccessPolicy accessPolicy;

    @Test
    void anonymousCannotReadHistory() throws Exception {
        mvc.perform(get("/api/reservations/1/history")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "TOURIST")
    void deniesForeignReservation() throws Exception {
        mvc.perform(get("/api/reservations/1/history")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "TOURIST")
    void ownerReadsSameScopeAsReservationWithNoStore() throws Exception {
        when(accessPolicy.readReservation(1L)).thenReturn(true);
        when(service.list(1L, 0, 20)).thenReturn(new ReservationHistoryService.HistoryPage(List.of(), 0, 0, 20, 0));
        mvc.perform(get("/api/reservations/1/history")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsUnboundedPagination() throws Exception {
        mvc.perform(get("/api/reservations/1/history").param("size", "101")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
