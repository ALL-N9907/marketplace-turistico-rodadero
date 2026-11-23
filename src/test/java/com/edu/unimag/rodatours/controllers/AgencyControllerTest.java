package com.edu.unimag.rodatours.controllers;

import com.edu.unimag.rodatours.api.controllers.AgencyController;
import com.edu.unimag.rodatours.api.dtos.request.UpdateAgencyRequest;
import com.edu.unimag.rodatours.api.dtos.response.AgencyResponse;
import com.edu.unimag.rodatours.domain.entities.enums.AgencyStatus;
import com.edu.unimag.rodatours.services.AgencyService;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgencyController.class)
@org.springframework.context.annotation.Import(com.edu.unimag.rodatours.config.SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class AgencyControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AgencyService service;

    private final AgencyResponse agency = new AgencyResponse(1L, "Roda Tours", "900123",
            null, "Rodadero", "3001234567", AgencyStatus.ACTIVE);

    @Test
    void createsWithLocation() throws Exception {
        when(service.create(any())).thenReturn(agency);
        mvc.perform(post("/api/agencies").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombreComercial":"Roda Tours","nitORegistro":"900123",
                         "direccion":"Rodadero","telefonoContacto":"3001234567"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/agencies/1"))
                .andExpect(jsonPath("$.nombreComercial").value("Roda Tours"));
    }

    @Test
    void readsUpdatesAndFiltersAgencies() throws Exception {
        when(service.getById(1L)).thenReturn(agency);
        when(service.update(eq(1L), any(UpdateAgencyRequest.class))).thenReturn(agency);
        when(service.searchActiveByNameOrLocation("Rodadero")).thenReturn(List.of(agency));
        mvc.perform(get("/api/agencies/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(put("/api/agencies/1").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombreComercial":"Roda Tours","nitORegistro":"900123",
                         "direccion":"Rodadero","telefonoContacto":"3001234567"}
                        """))
                .andExpect(status().isOk());
        mvc.perform(get("/api/agencies").param("query", "Rodadero"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(get("/api/agencies").param("activeOnly", "true")).andExpect(status().isOk());
        mvc.perform(get("/api/agencies")).andExpect(status().isOk());
        verify(service).getAllActive();
        verify(service).getAll();
    }

    @Test
    void rejectsInvalidInputs() throws Exception {
        mvc.perform(post("/api/agencies").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.nombreComercial").exists());
        mvc.perform(get("/api/agencies/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/agencies").param("query", " ")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void mapsMissingAgencyAndDuplicateRegistration() throws Exception {
        when(service.getById(1L)).thenThrow(new ResourceNotFoundException("Agencia no encontrada"));
        when(service.create(any())).thenThrow(new ConflictException("NIT duplicado"));
        mvc.perform(get("/api/agencies/1")).andExpect(status().isNotFound());
        mvc.perform(post("/api/agencies").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombreComercial":"Roda Tours","nitORegistro":"900123",
                         "direccion":"Rodadero","telefonoContacto":"3001234567"}
                        """))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("NIT duplicado"));
    }
}
