package com.edu.unimag.rodatours.controllers;

import com.edu.unimag.rodatours.api.controllers.AvailabilityController;
import com.edu.unimag.rodatours.api.dtos.response.AvailabilitySlotResponse;
import com.edu.unimag.rodatours.services.AvailabilityService;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AvailabilityController.class)
@WithMockUser
class AvailabilityControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AvailabilityService service;

    private final LocalDate date = LocalDate.of(2027, 1, 15);

    @Test
    void returnsAvailableSlotsForRequestedPartySize() throws Exception {
        when(service.getAvailableSlots(1L, date, 2)).thenReturn(List.of(new AvailabilitySlotResponse(
                3L, 1L, date, LocalTime.of(8, 0), date.atTime(10, 0), 10, 5)));
        mvc.perform(get("/api/availability/tours/1").param("fecha", date.toString()).param("cantidadPersonas", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].horarioId").value(3))
                .andExpect(jsonPath("$[0].cuposDisponibles").value(5));
        verify(service).getAvailableSlots(1L, date, 2);
    }

    @Test
    void defaultsToOnePersonAndReturnsEmptyList() throws Exception {
        when(service.getAvailableSlots(1L, date, 1)).thenReturn(List.of());
        mvc.perform(get("/api/availability/tours/1").param("fecha", date.toString()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).getAvailableSlots(1L, date, 1);
    }

    @Test
    void rejectsMissingMalformedDateAndNonpositiveNumbers() throws Exception {
        mvc.perform(get("/api/availability/tours/1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/availability/tours/1").param("fecha", "15/01/2027"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/availability/tours/1").param("fecha", date.toString()).param("cantidadPersonas", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/availability/tours/0").param("fecha", date.toString()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void returnsNotFoundForUnknownTour() throws Exception {
        when(service.getAvailableSlots(1L, date, 1)).thenThrow(new ResourceNotFoundException("Tour no encontrado"));
        mvc.perform(get("/api/availability/tours/1").param("fecha", date.toString()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Tour no encontrado"));
    }
}
