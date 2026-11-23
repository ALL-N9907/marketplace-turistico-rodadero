package com.edu.unimag.rodatours.controllers;

import com.edu.unimag.rodatours.api.controllers.ExperienceTypeController;
import com.edu.unimag.rodatours.api.controllers.GuideController;
import com.edu.unimag.rodatours.api.controllers.TourController;
import com.edu.unimag.rodatours.api.controllers.TourScheduleController;
import com.edu.unimag.rodatours.api.dtos.response.ExperienceTypeResponse;
import com.edu.unimag.rodatours.api.dtos.response.GuideResponse;
import com.edu.unimag.rodatours.api.dtos.response.TourResponse;
import com.edu.unimag.rodatours.api.dtos.response.TourScheduleResponse;
import com.edu.unimag.rodatours.domain.entities.enums.DifficultyLevel;
import com.edu.unimag.rodatours.domain.entities.enums.GuideStatus;
import com.edu.unimag.rodatours.services.ExperienceTypeService;
import com.edu.unimag.rodatours.services.GuideService;
import com.edu.unimag.rodatours.services.TourScheduleService;
import com.edu.unimag.rodatours.services.TourService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ExperienceTypeController.class, GuideController.class, TourController.class, TourScheduleController.class})
@org.springframework.context.annotation.Import(com.edu.unimag.rodatours.config.SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class CatalogControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ExperienceTypeService experienceTypeService;
    @MockitoBean
    private GuideService guideService;
    @MockitoBean
    private TourService tourService;
    @MockitoBean
    private TourScheduleService scheduleService;

    @Test
    void createsAndListsExperienceTypes() throws Exception {
        ExperienceTypeResponse response = new ExperienceTypeResponse(1L, "Aventura", "Naturaleza");
        when(experienceTypeService.create(any())).thenReturn(response);
        when(experienceTypeService.getAll()).thenReturn(List.of(response));
        when(experienceTypeService.getById(1L)).thenReturn(response);
        mvc.perform(post("/api/experience-types").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Aventura\",\"descripcion\":\"Naturaleza\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/experience-types/1"));
        mvc.perform(get("/api/experience-types")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Aventura"));
        mvc.perform(get("/api/experience-types/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createsAndUpdatesTours() throws Exception {
        DifficultyLevel difficulty = DifficultyLevel.values()[0];
        TourResponse response = new TourResponse(1L, 2L, 3L, "Minca", "Caminata", "Minca",
                new BigDecimal("100000"), new BigDecimal("2"), difficulty, null, null);
        when(tourService.create(any())).thenReturn(response);
        when(tourService.update(eq(1L), any())).thenReturn(response);
        String body = """
                {"agenciaId":2,"tipoExperienciaId":3,"titulo":"Minca","descripcion":"Caminata",
                 "destino":"Minca","precio":100000,"duracionHoras":2,"nivelDificultad":"%s"}
                """.formatted(difficulty.name());
        mvc.perform(post("/api/tours").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/api/tours/1"))
                .andExpect(jsonPath("$.destino").value("Minca"));
        mvc.perform(put("/api/tours/1").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        verify(tourService).update(eq(1L), any());
    }

    @Test
    void createsSchedulesWithResolvableLocation() throws Exception {
        TourScheduleResponse response = new TourScheduleResponse(3L, 1L, null,
                LocalDate.of(2027, 1, 15), LocalTime.of(8, 0), null, 10, 10);
        when(scheduleService.create(eq(1L), any())).thenReturn(response);
        when(scheduleService.getById(3L)).thenReturn(response);
        mvc.perform(post("/api/tours/1/schedules").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaTour\":\"2027-01-15\",\"horaInicio\":\"08:00\",\"cuposTotales\":10}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/api/schedules/3"))
                .andExpect(jsonPath("$.tourId").value(1));
        mvc.perform(get("/api/schedules/3")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void createsAndUpdatesGuides() throws Exception {
        GuideResponse response = new GuideResponse(1L, 2L, "Luis", "123", "RNT123",
                "luis@example.com", "3001234567", null, GuideStatus.ACTIVE, null);
        when(guideService.create(any())).thenReturn(response);
        when(guideService.update(eq(1L), any())).thenReturn(response);
        String body = """
                {"agenciaId":2,"nombreCompleto":"Luis","documentoIdentidad":"123","registroGuia":"RNT123",
                 "correoElectronico":"luis@example.com","telefono":"3001234567"}
                """;
        mvc.perform(post("/api/guides").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/api/guides/1"))
                .andExpect(jsonPath("$.nombreCompleto").value("Luis"));
        mvc.perform(put("/api/guides/1").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        verify(guideService).update(eq(1L), any());
    }

    @Test
    void listsToursAndAppliesAgencyFilters() throws Exception {
        mvc.perform(get("/api/tours")).andExpect(status().isOk());
        mvc.perform(get("/api/tours/1")).andExpect(status().isOk());
        mvc.perform(get("/api/tours").param("agenciaId", "1").param("destino", "Minca"))
                .andExpect(status().isOk());
        verify(tourService).getAll();
        verify(tourService).getById(1L);
        verify(tourService).getByAgency(1L, null, "Minca");
    }

    @Test
    void rejectsUnsupportedTourFilterCombinationsAndInvalidEnums() throws Exception {
        mvc.perform(get("/api/tours").param("destino", "Minca")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tours").param("agenciaId", "1").param("dificultad", "INVALID"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/tours").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(tourService);
    }

    @Test
    void listsSchedulesAndAssignsGuide() throws Exception {
        mvc.perform(get("/api/tours/1/schedules").param("fecha", "2027-01-15"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/schedules/3")).andExpect(status().isOk());
        mvc.perform(put("/api/schedules/3/guide").with(csrf()).param("guiaId", "2"))
                .andExpect(status().isOk());
        verify(scheduleService).getByTourId(1L, LocalDate.of(2027, 1, 15));
        verify(scheduleService).getById(3L);
        verify(scheduleService).assignGuide(3L, 2L);
    }

    @Test
    void rejectsInvalidScheduleAndGuideAssignment() throws Exception {
        mvc.perform(post("/api/tours/1/schedules").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaTour\":\"2027-01-15\",\"horaInicio\":\"08:00\",\"cuposTotales\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/schedules/3/guide").with(csrf()).param("guiaId", "0"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(scheduleService);
    }

    @Test
    void listsAndFiltersGuides() throws Exception {
        mvc.perform(get("/api/guides")).andExpect(status().isOk());
        mvc.perform(get("/api/guides/1")).andExpect(status().isOk());
        mvc.perform(get("/api/guides").param("agenciaId", "1").param("estado", "ACTIVE"))
                .andExpect(status().isOk());
        verify(guideService).getAll();
        verify(guideService).getById(1L);
        verify(guideService).getByAgencyAndStatus(1L, GuideStatus.ACTIVE);
    }

    @Test
    void rejectsIncompleteGuideFiltersAndInvalidCreation() throws Exception {
        mvc.perform(get("/api/guides").param("agenciaId", "1")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/guides").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(guideService);
    }
}
