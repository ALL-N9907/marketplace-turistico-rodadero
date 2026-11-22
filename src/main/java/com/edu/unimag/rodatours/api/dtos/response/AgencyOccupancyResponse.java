package com.edu.unimag.rodatours.api.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AgencyOccupancyResponse(
        Long agenciaId,
        String nombreComercial,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        Long cuposTotales,
        Long cuposOcupados,
        BigDecimal porcentajeOcupacion
) {
}
