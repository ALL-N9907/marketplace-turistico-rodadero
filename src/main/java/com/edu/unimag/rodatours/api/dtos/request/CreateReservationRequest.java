package com.edu.unimag.rodatours.api.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(
        @NotNull(message = "El turista es obligatorio")
        Long turistaId,

        @NotNull(message = "El horario de salida es obligatorio")
        Long horarioId,

        @NotNull(message = "La cantidad de personas es obligatoria")
        @Min(value = 1, message = "Debe reservarse al menos para 1 persona")
        Integer cantidadPersonas,

        String observaciones
) {
}
