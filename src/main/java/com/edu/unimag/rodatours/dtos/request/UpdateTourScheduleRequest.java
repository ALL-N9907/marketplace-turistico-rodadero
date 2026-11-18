package com.edu.unimag.rodatours.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateTourScheduleRequest(
        Long guiaId,

        @NotNull(message = "La fecha del tour es obligatoria")
        LocalDate fechaTour,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "El total de cupos es obligatorio")
        @Min(value = 1, message = "El cupo total debe ser al menos 1")
        Integer cuposTotales
) {
}
