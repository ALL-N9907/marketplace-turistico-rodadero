package com.edu.unimag.rodatours.dtos.request;

import com.edu.unimag.rodatours.entities.enums.TouristStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTouristRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres")
        String nombreCompleto,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "Debe proporcionar un formato de correo electrónico válido")
        @Size(max = 255, message = "El correo electrónico no puede superar los 255 caracteres")
        String correoElectronico,

        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String telefono,

        TouristStatus estado
) {
}
