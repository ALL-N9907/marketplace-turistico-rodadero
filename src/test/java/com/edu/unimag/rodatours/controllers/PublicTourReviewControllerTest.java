package com.edu.unimag.rodatours.controllers;

import com.edu.unimag.rodatours.api.controllers.PublicTourReviewController;
import com.edu.unimag.rodatours.config.SecurityConfig;
import com.edu.unimag.rodatours.services.PublicTourReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicTourReviewController.class)
@Import(SecurityConfig.class)
class PublicTourReviewControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean PublicTourReviewService service;

    @Test
    void listsWithoutLoginAndDoesNotExposeReservationOrContactData() throws Exception {
        when(service.list(1L, 0, 5)).thenReturn(new PublicTourReviewService.ReviewPage(
                List.of(new PublicTourReviewService.PublicReview(8L, 5, "Excelente", LocalDateTime.now())), 1, 0, 5, 1, 5.0));
        mvc.perform(get("/api/tours/1/reviews"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.averageRating").value(5.0))
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(content().string(not(containsString("reservaId"))))
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[0].touristId").doesNotExist());
    }

    @Test
    void validatesPaginationAndTourId() throws Exception {
        mvc.perform(get("/api/tours/0/reviews")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tours/1/reviews").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tours/1/reviews").param("size", "101")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
