package com.edu.unimag.rodatours.api.dtos.response;

public record AgencyProductivityResponse(
        Long agenciaId,
        String nombreComercial,
        Long toursCompletados,
        Integer posicion
) {
}
