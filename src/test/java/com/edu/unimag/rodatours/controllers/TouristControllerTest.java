package com.edu.unimag.rodatours.controllers;

import com.edu.unimag.rodatours.api.controllers.TouristController;
import com.edu.unimag.rodatours.api.dtos.response.TouristResponse;
import com.edu.unimag.rodatours.domain.entities.enums.TouristStatus;
import com.edu.unimag.rodatours.services.TouristService;
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

@WebMvcTest(TouristController.class)
@WithMockUser
class TouristControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TouristService service;

    private final TouristResponse tourist = new TouristResponse(1L, "Ana Pérez", "ana@example.com",
            null, TouristStatus.ACTIVE, null);

    @Test
    void createsTouristWithoutExposingPassword() throws Exception {
        when(service.create(any())).thenReturn(tourist);
        mvc.perform(post("/api/tourists").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombreCompleto":"Ana Pérez","correoElectronico":"ana@example.com","contrasena":"secreto"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/tourists/1"))
                .andExpect(jsonPath("$.correoElectronico").value("ana@example.com"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
    }

    @Test
    void readsListsFiltersAndUpdates() throws Exception {
        when(service.getById(1L)).thenReturn(tourist);
        when(service.getAll()).thenReturn(List.of(tourist));
        when(service.getByStatus(TouristStatus.ACTIVE)).thenReturn(List.of(tourist));
        when(service.update(eq(1L), any())).thenReturn(tourist);
        mvc.perform(get("/api/tourists/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(get("/api/tourists")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(get("/api/tourists").param("estado", "ACTIVE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].estado").value("ACTIVE"));
        mvc.perform(put("/api/tourists/1").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombreCompleto":"Ana Pérez","correoElectronico":"ana@example.com"}
                        """))
                .andExpect(status().isOk());
        verify(service).getByStatus(TouristStatus.ACTIVE);
        verify(service).update(eq(1L), any());
    }

    @Test
    void rejectsInvalidEmailMissingFieldsAndInvalidIds() throws Exception {
        mvc.perform(post("/api/tourists").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correoElectronico\":\"invalido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.correoElectronico").exists())
                .andExpect(jsonPath("$.errors.contrasena").exists());
        mvc.perform(get("/api/tourists/-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tourists").param("estado", "INVALID"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void mapsMissingTouristAndDuplicateEmail() throws Exception {
        when(service.getById(1L)).thenThrow(new ResourceNotFoundException("Turista no encontrado"));
        when(service.create(any())).thenThrow(new ConflictException("Correo duplicado"));
        mvc.perform(get("/api/tourists/1")).andExpect(status().isNotFound());
        mvc.perform(post("/api/tourists").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombreCompleto":"Ana Pérez","correoElectronico":"ana@example.com","contrasena":"secreto"}
                        """))
                .andExpect(status().isConflict());
    }
}
