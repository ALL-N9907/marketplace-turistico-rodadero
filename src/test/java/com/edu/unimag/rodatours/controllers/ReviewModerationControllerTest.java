package com.edu.unimag.rodatours.controllers;

import com.edu.unimag.rodatours.api.controllers.ReviewModerationController;
import com.edu.unimag.rodatours.config.SecurityConfig;
import com.edu.unimag.rodatours.domain.entities.Review;
import com.edu.unimag.rodatours.domain.repositories.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewModerationController.class) @Import(SecurityConfig.class)
class ReviewModerationControllerTest {
    @Autowired MockMvc mvc;@MockitoBean ReviewRepository reviews;
    @Test @WithMockUser(roles="TOURIST") void touristsCannotModerate()throws Exception{mvc.perform(put("/api/admin/reviews/1/moderation").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"published\":false,\"reason\":\"No permitido\"}")).andExpect(status().isForbidden());verifyNoInteractions(reviews);}
    @Test @WithMockUser(roles="ADMIN") void adminHidesReviewWithReason()throws Exception{when(reviews.findById(1L)).thenReturn(Optional.of(Review.builder().id(1L).calificacion(5).build()));mvc.perform(put("/api/admin/reviews/1/moderation").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"published\":false,\"reason\":\"Datos personales\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.published").value(false));}
    @Test @WithMockUser(roles="ADMIN") void reasonRequired()throws Exception{mvc.perform(put("/api/admin/reviews/1/moderation").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"published\":false,\"reason\":\" \"}")).andExpect(status().isBadRequest());verifyNoInteractions(reviews);}
}
