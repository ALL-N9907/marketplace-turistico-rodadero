package com.edu.unimag.rodatours.dtos.request;

import com.edu.unimag.rodatours.entities.enums.GuideStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateGuideRequest(
        @NotNull(message = "La agencia a la que pertenece el guía es obligatoria")
        Long agenciaId,

        @NotBlank(message = "El nombre completo del guía es obligatorio")
        @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres")
        String nombreCompleto,

        @NotBlank(message = "El documento de identidad es obligatorio")
        @Size(max = 30, message = "El documento no puede superar los 30 caracteres")
        String documentoIdentidad,

        @NotBlank(message = "El registro o tarjeta profesional es obligatorio")
        @Size(max = 30, message = "El registro no puede superar los 30 caracteres")
        String registroGuia,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "Debe proporcionar un formato de correo electrónico válido")
        @Size(max = 255, message = "El correo electrónico no puede superar los 255 caracteres")
        String correoElectronico,

        @NotBlank(message = "El teléfono del guía es obligatorio")
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String telefono,

        @Size(max = 100, message = "Los idiomas no pueden superar los 100 caracteres")
        String idiomas,

        GuideStatus estado
) {
}
