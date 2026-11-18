package com.edu.unimag.rodatours.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExperienceTypeRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
        String nombre,

        String descripcion
) {
}
