package com.edu.unimag.rodatours.dtos.response;

import com.edu.unimag.rodatours.entities.enums.TouristStatus;

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
