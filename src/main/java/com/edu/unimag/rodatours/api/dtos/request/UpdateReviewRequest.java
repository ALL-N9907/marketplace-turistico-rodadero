package com.edu.unimag.rodatours.api.dtos.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateReviewRequest(
        @NotNull(message = "La calificación es obligatoria")
        @Min(value = 1, message = "La calificación mínima es 1")
        @Max(value = 5, message = "La calificación máxima es 5")
        Integer calificacion,

        String comentario
) {
}
