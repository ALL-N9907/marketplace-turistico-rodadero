package com.edu.unimag.rodatours.dtos.request;

import com.edu.unimag.rodatours.entities.enums.DifficultyLevel;
import com.edu.unimag.rodatours.entities.enums.TourStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateTourRequest(
        @NotNull(message = "La agencia que ofrece el tour es obligatoria")
        Long agenciaId,

        @NotNull(message = "El tipo de experiencia es obligatorio")
        Long tipoExperienciaId,

        @NotBlank(message = "El título del tour es obligatorio")
        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        String titulo,

        @NotBlank(message = "La descripción del tour es obligatoria")
        String descripcion,

        @NotBlank(message = "El destino del tour es obligatorio")
        @Size(max = 120, message = "El destino no puede superar los 120 caracteres")
        String destino,

        @NotNull(message = "El precio por persona es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
        BigDecimal precio,

        @NotNull(message = "La duración en horas es obligatoria")
        @DecimalMin(value = "0.1", message = "La duración en horas debe ser mayor que cero")
        BigDecimal duracionHoras,

        @NotNull(message = "El nivel de dificultad es obligatorio")
        DifficultyLevel nivelDificultad,

        String serviciosIncluidos,

        TourStatus estado
) {
}
