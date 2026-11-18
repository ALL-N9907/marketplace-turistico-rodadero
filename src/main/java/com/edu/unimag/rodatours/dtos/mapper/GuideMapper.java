package com.edu.unimag.rodatours.dtos.mapper;

import com.edu.unimag.rodatours.dtos.request.CreateGuideRequest;
import com.edu.unimag.rodatours.dtos.request.UpdateGuideRequest;
import com.edu.unimag.rodatours.dtos.response.GuideResponse;
import com.edu.unimag.rodatours.entities.Agency;
import com.edu.unimag.rodatours.entities.Guide;
import com.edu.unimag.rodatours.entities.enums.GuideStatus;

public final class GuideMapper {

    private GuideMapper() {
    }

    public static Guide toEntity(CreateGuideRequest dto, Agency agencia) {
        if (dto == null) {
            return null;
        }
        return Guide.builder()
                .agencia(agencia)
                .nombreCompleto(dto.nombreCompleto())
                .documentoIdentidad(dto.documentoIdentidad())
                .registroGuia(dto.registroGuia())
                .correoElectronico(dto.correoElectronico())
                .telefono(dto.telefono())
                .idiomas(dto.idiomas())
                .estado(dto.estado() == null ? GuideStatus.ACTIVE : dto.estado())
                .build();
    }

    public static GuideResponse toResponse(Guide entity) {
        if (entity == null) {
            return null;
        }
        return new GuideResponse(
                entity.getId(),
                entity.getAgencia() == null ? null : entity.getAgencia().getId(),
                entity.getNombreCompleto(),
                entity.getDocumentoIdentidad(),
                entity.getRegistroGuia(),
                entity.getCorreoElectronico(),
                entity.getTelefono(),
                entity.getIdiomas(),
                entity.getEstado(),
                entity.getFechaCreacion()
        );
    }

    public static void applyUpdate(UpdateGuideRequest dto, Guide entity, Agency agencia) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setAgencia(agencia);
        entity.setNombreCompleto(dto.nombreCompleto());
        entity.setDocumentoIdentidad(dto.documentoIdentidad());
        entity.setRegistroGuia(dto.registroGuia());
        entity.setCorreoElectronico(dto.correoElectronico());
        entity.setTelefono(dto.telefono());
        entity.setIdiomas(dto.idiomas());
        if (dto.estado() != null) {
            entity.setEstado(dto.estado());
        }
    }
}
