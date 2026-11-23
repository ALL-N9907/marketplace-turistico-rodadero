package com.edu.unimag.rodatours.controllers;

import com.edu.unimag.rodatours.api.controllers.ReportController;
import com.edu.unimag.rodatours.services.ReportService;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@WithMockUser
class ReportControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReportService service;

    private final LocalDate desde = LocalDate.of(2026, 1, 1);
    private final LocalDate hasta = LocalDate.of(2026, 1, 31);

    @Test
    void delegatesAllThreeReports() throws Exception {
        when(service.getAgencyProductivityRanking(desde, hasta)).thenReturn(List.of());
        when(service.getNoShowTouristsReport(desde, hasta)).thenReturn(List.of());
        mvc.perform(get("/api/reports/agency-occupancy").param("agenciaId", "1")
                        .param("desde", desde.toString()).param("hasta", hasta.toString()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/reports/agency-productivity")
                        .param("desde", desde.toString()).param("hasta", hasta.toString()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/reports/no-show-tourists")
                        .param("desde", desde.toString()).param("hasta", hasta.toString()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).getAgencyOccupancy(1L, desde, hasta);
        verify(service).getAgencyProductivityRanking(desde, hasta);
        verify(service).getNoShowTouristsReport(desde, hasta);
    }

    @Test
    void rejectsMissingReversedAndMalformedRanges() throws Exception {
        mvc.perform(get("/api/reports/agency-productivity")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/reports/agency-productivity")
                        .param("desde", hasta.toString()).param("hasta", desde.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/reports/no-show-tourists").param("desde", "invalid").param("hasta", hasta.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/reports/agency-occupancy").param("agenciaId", "0")
                        .param("desde", desde.toString()).param("hasta", hasta.toString()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void returnsNotFoundForMissingAgency() throws Exception {
        when(service.getAgencyOccupancy(1L, desde, hasta)).thenThrow(new ResourceNotFoundException("Agencia inexistente"));
        mvc.perform(get("/api/reports/agency-occupancy").param("agenciaId", "1")
                        .param("desde", desde.toString()).param("hasta", hasta.toString()))
                .andExpect(status().isNotFound());
    }
}
