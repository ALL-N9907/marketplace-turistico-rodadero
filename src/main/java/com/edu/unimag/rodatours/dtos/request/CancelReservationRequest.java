package com.edu.unimag.rodatours.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record CancelReservationRequest(
        @NotBlank(message = "El motivo de cancelación es obligatorio")
        String motivo
) {
}
