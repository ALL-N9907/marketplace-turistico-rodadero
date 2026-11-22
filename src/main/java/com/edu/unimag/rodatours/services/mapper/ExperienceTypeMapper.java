package com.edu.unimag.rodatours.services.mapper;

import com.edu.unimag.rodatours.api.dtos.request.CreateExperienceTypeRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateExperienceTypeRequest;
import com.edu.unimag.rodatours.api.dtos.response.ExperienceTypeResponse;
import com.edu.unimag.rodatours.domain.entities.ExperienceType;

public final class ExperienceTypeMapper {

    private ExperienceTypeMapper() {
    }

    public static ExperienceType toEntity(CreateExperienceTypeRequest dto) {
        if (dto == null) {
            return null;
        }
        return ExperienceType.builder()
                .nombre(dto.nombre())
                .descripcion(dto.descripcion())
                .build();
    }

    public static ExperienceTypeResponse toResponse(ExperienceType entity) {
        if (entity == null) {
            return null;
        }
        return new ExperienceTypeResponse(entity.getId(), entity.getNombre(), entity.getDescripcion());
    }

    public static void applyUpdate(UpdateExperienceTypeRequest dto, ExperienceType entity) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setNombre(dto.nombre());
        entity.setDescripcion(dto.descripcion());
    }
}
