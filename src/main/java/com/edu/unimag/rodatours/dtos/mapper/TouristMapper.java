package com.edu.unimag.rodatours.dtos.mapper;

import com.edu.unimag.rodatours.dtos.request.CreateTouristRequest;
import com.edu.unimag.rodatours.dtos.request.UpdateTouristRequest;
import com.edu.unimag.rodatours.dtos.response.TouristResponse;
import com.edu.unimag.rodatours.entities.Tourist;
import com.edu.unimag.rodatours.entities.enums.TouristStatus;

/**
 * Conversión entre {@link Tourist} y sus DTOs. La contraseña nunca se expone en una respuesta.
 */
public final class TouristMapper {

    private TouristMapper() {
    }

    public static Tourist toEntity(CreateTouristRequest dto) {
        if (dto == null) {
            return null;
        }
        return Tourist.builder()
                .nombreCompleto(dto.nombreCompleto())
                .correoElectronico(dto.correoElectronico())
                .contrasena(dto.contrasena())
                .telefono(dto.telefono())
                .estado(dto.estado() == null ? TouristStatus.ACTIVE : dto.estado())
                .build();
    }

    public static TouristResponse toResponse(Tourist entity) {
        if (entity == null) {
            return null;
        }
        return new TouristResponse(
                entity.getId(),
                entity.getNombreCompleto(),
                entity.getCorreoElectronico(),
                entity.getTelefono(),
                entity.getEstado(),
                entity.getFechaCreacion()
        );
    }

    public static void applyUpdate(UpdateTouristRequest dto, Tourist entity) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setNombreCompleto(dto.nombreCompleto());
        entity.setCorreoElectronico(dto.correoElectronico());
        entity.setTelefono(dto.telefono());
        if (dto.estado() != null) {
            entity.setEstado(dto.estado());
        }
    }
}
