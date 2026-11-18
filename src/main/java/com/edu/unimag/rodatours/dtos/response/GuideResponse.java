package com.edu.unimag.rodatours.dtos.response;

import com.edu.unimag.rodatours.entities.enums.GuideStatus;

import java.time.LocalDateTime;

public record GuideResponse(
        Long id,
        Long agenciaId,
        String nombreCompleto,
        String documentoIdentidad,
        String registroGuia,
        String correoElectronico,
        String telefono,
        String idiomas,
        GuideStatus estado,
        LocalDateTime fechaCreacion
) {
}
