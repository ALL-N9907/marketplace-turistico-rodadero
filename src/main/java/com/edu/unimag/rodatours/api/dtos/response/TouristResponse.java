package com.edu.unimag.rodatours.api.dtos.response;

import com.edu.unimag.rodatours.domain.entities.enums.TouristStatus;

import java.time.LocalDateTime;

public record TouristResponse(
        Long id,
        String nombreCompleto,
        String correoElectronico,
        String telefono,
        TouristStatus estado,
        LocalDateTime fechaCreacion
) {
}
