package com.edu.unimag.rodatours.dtos.response;

import com.edu.unimag.rodatours.entities.enums.AgencyStatus;

public record AgencyResponse(
        Long id,
        String nombreComercial,
        String nitORegistro,
        String descripcion,
        String direccion,
        String telefonoContacto,
        AgencyStatus estado
) {
}
