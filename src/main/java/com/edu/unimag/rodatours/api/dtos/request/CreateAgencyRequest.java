package com.edu.unimag.rodatours.api.dtos.request;

import com.edu.unimag.rodatours.domain.entities.enums.AgencyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAgencyRequest(
        @NotBlank(message = "El nombre comercial de la agencia es obligatorio")
        @Size(max = 150, message = "El nombre comercial no puede superar los 150 caracteres")
        String nombreComercial,

        @NotBlank(message = "El NIT o registro legal es obligatorio")
        @Size(max = 30, message = "El NIT o registro no puede superar los 30 caracteres")
        String nitORegistro,

        String descripcion,

        @NotBlank(message = "La dirección de la agencia es obligatoria")
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
        String direccion,

        @NotBlank(message = "El teléfono de contacto es obligatorio")
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String telefonoContacto,

        AgencyStatus estado
) {
}
