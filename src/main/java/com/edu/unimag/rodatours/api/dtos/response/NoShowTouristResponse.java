package com.edu.unimag.rodatours.api.dtos.response;

import java.time.LocalDate;

public record NoShowTouristResponse(
        Long turistaId,
        String nombreCompleto,
        Long cantidadNoShow,
        LocalDate fechaDesde,
        LocalDate fechaHasta
) {
}
