package com.edu.unimag.rodatours.services.mapper;

import com.edu.unimag.rodatours.api.dtos.request.CreateAgencyRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateAgencyRequest;
import com.edu.unimag.rodatours.api.dtos.response.AgencyResponse;
import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.enums.AgencyStatus;

public final class AgencyMapper {

    private AgencyMapper() {
    }

    public static Agency toEntity(CreateAgencyRequest dto) {
        if (dto == null) {
            return null;
        }
        return Agency.builder()
                .nombreComercial(dto.nombreComercial())
                .nitORegistro(dto.nitORegistro())
                .descripcion(dto.descripcion())
                .direccion(dto.direccion())
                .telefonoContacto(dto.telefonoContacto())
                .estado(dto.estado() == null ? AgencyStatus.ACTIVE : dto.estado())
                .build();
    }

    public static AgencyResponse toResponse(Agency entity) {
        if (entity == null) {
            return null;
        }
        return new AgencyResponse(
                entity.getId(),
                entity.getNombreComercial(),
                entity.getNitORegistro(),
                entity.getDescripcion(),
                entity.getDireccion(),
                entity.getTelefonoContacto(),
                entity.getEstado()
        );
    }

    public static void applyUpdate(UpdateAgencyRequest dto, Agency entity) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setNombreComercial(dto.nombreComercial());
        entity.setNitORegistro(dto.nitORegistro());
        entity.setDescripcion(dto.descripcion());
        entity.setDireccion(dto.direccion());
        entity.setTelefonoContacto(dto.telefonoContacto());
        if (dto.estado() != null) {
            entity.setEstado(dto.estado());
        }
    }
}
