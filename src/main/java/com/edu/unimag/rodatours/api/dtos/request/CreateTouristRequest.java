package com.edu.unimag.rodatours.api.dtos.request;

import com.edu.unimag.rodatours.domain.entities.enums.TouristStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTouristRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres")
        String nombreCompleto,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "Debe proporcionar un formato de correo electrónico válido")
        @Size(max = 255, message = "El correo electrónico no puede superar los 255 caracteres")
        String correoElectronico,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 255, message = "La contraseña no puede superar los 255 caracteres")
        String contrasena,

        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String telefono,

        TouristStatus estado
) {
}
