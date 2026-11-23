package com.edu.unimag.rodatours.controllers;

import com.edu.unimag.rodatours.api.controllers.ReservationController;
import com.edu.unimag.rodatours.api.dtos.request.CancelReservationRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateReservationRequest;
import com.edu.unimag.rodatours.api.dtos.response.ReservationResponse;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.services.ReservationService;
import com.edu.unimag.rodatours.services.exceptions.BusinessException;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
@WithMockUser
class ReservationControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReservationService service;

    private ReservationResponse reservation(ReservationStatus status) {
        return new ReservationResponse(1L, 2L, 3L, null, null, null, 2,
                new BigDecimal("200000"), status, null, null, "qr-1", null);
    }

    @Test
    void createsScheduledReservationWithLocation() throws Exception {
        when(service.create(any())).thenReturn(reservation(ReservationStatus.SCHEDULED));
        mvc.perform(post("/api/reservations").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"turistaId":2,"horarioId":3,"cantidadPersonas":2,"observaciones":"Sin gluten"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/reservations/1"))
                .andExpect(jsonPath("$.estado").value("SCHEDULED"));
        verify(service).create(new CreateReservationRequest(2L, 3L, 2, "Sin gluten"));
    }

    @Test
    void rejectsInvalidCreationWithoutCallingService() throws Exception {
        mvc.perform(post("/api/reservations").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidadPersonas\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.turistaId").exists())
                .andExpect(jsonPath("$.errors.horarioId").exists())
                .andExpect(jsonPath("$.errors.cantidadPersonas").exists());
        verifyNoInteractions(service);
    }

    @Test
    void getsReservationAndListsByTouristAndStatus() throws Exception {
        when(service.getById(1L)).thenReturn(reservation(ReservationStatus.CONFIRMED));
        when(service.getByTourist(2L, ReservationStatus.CONFIRMED))
                .thenReturn(List.of(reservation(ReservationStatus.CONFIRMED)));
        mvc.perform(get("/api/reservations/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(get("/api/reservations").param("turistaId", "2").param("estado", "CONFIRMED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].estado").value("CONFIRMED"));
    }

    @Test
    void listsAllByScheduleAndDateRange() throws Exception {
        mvc.perform(get("/api/reservations")).andExpect(status().isOk());
        mvc.perform(get("/api/reservations").param("horarioId", "3")).andExpect(status().isOk());
        mvc.perform(get("/api/reservations").param("desde", "2026-01-01T00:00:00")
                        .param("hasta", "2026-01-31T23:59:59"))
                .andExpect(status().isOk());
        verify(service).getAll();
        verify(service).getBySchedule(3L);
        verify(service).getByReservationDateRange(java.time.LocalDateTime.parse("2026-01-01T00:00:00"),
                java.time.LocalDateTime.parse("2026-01-31T23:59:59"));
    }

    @Test
    void rejectsAmbiguousIncompleteAndInvalidFilters() throws Exception {
        mvc.perform(get("/api/reservations").param("estado", "CONFIRMED"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/reservations").param("turistaId", "2").param("horarioId", "3"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/reservations").param("desde", "2026-01-01T00:00:00"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/reservations").param("turistaId", "2").param("estado", "INVALID"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/reservations/0")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void confirmsReservation() throws Exception {
        when(service.confirm(1L)).thenReturn(reservation(ReservationStatus.CONFIRMED));
        mvc.perform(put("/api/reservations/1/confirm").with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMED"));
        verify(service).confirm(1L);
    }

    @Test
    void cancelsWithMandatoryReason() throws Exception {
        when(service.cancel(eq(1L), any())).thenReturn(reservation(ReservationStatus.CANCELLED));
        mvc.perform(put("/api/reservations/1/cancel").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Cambio de planes\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELLED"));
        verify(service).cancel(1L, new CancelReservationRequest("Cambio de planes"));
    }

    @Test
    void rejectsBlankCancellationReasonAndMalformedJson() throws Exception {
        mvc.perform(put("/api/reservations/1/cancel").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"  \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.motivo").exists());
        mvc.perform(post("/api/reservations").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void completesWithOptionalNotesAndMarksNoShow() throws Exception {
        when(service.complete(1L, "Asistió")).thenReturn(reservation(ReservationStatus.COMPLETED));
        when(service.markNoShow(1L)).thenReturn(reservation(ReservationStatus.NO_SHOW));
        mvc.perform(put("/api/reservations/1/complete").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observaciones\":\"Asistió\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("COMPLETED"));
        mvc.perform(put("/api/reservations/1/complete").with(csrf())).andExpect(status().isOk());
        mvc.perform(put("/api/reservations/1/no-show").with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("NO_SHOW"));
        verify(service).complete(1L, null);
    }

    @Test
    void mapsDomainErrorsToHttpResponses() throws Exception {
        when(service.getById(1L)).thenThrow(new ResourceNotFoundException("Reserva no encontrada"));
        when(service.confirm(1L)).thenThrow(new ConflictException("Estado incompatible"));
        when(service.markNoShow(1L)).thenThrow(new BusinessException("Fecha futura"));
        mvc.perform(get("/api/reservations/1")).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Reserva no encontrada"));
        mvc.perform(put("/api/reservations/1/confirm").with(csrf())).andExpect(status().isConflict());
        mvc.perform(put("/api/reservations/1/no-show").with(csrf())).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMutationWithoutCsrf() throws Exception {
        mvc.perform(put("/api/reservations/1/confirm")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
