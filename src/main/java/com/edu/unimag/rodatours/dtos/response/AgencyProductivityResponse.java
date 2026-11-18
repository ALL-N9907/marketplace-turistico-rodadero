package com.edu.unimag.rodatours.dtos.response;

public record AgencyProductivityResponse(
        Long agenciaId,
        String nombreComercial,
        Long toursCompletados,
        Integer posicion
) {
}
