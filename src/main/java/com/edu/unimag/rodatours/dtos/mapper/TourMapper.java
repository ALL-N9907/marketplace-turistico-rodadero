package com.edu.unimag.rodatours.dtos.mapper;

import com.edu.unimag.rodatours.dtos.request.CreateTourRequest;
import com.edu.unimag.rodatours.dtos.request.UpdateTourRequest;
import com.edu.unimag.rodatours.dtos.response.TourResponse;
import com.edu.unimag.rodatours.entities.Agency;
import com.edu.unimag.rodatours.entities.ExperienceType;
import com.edu.unimag.rodatours.entities.Tour;
import com.edu.unimag.rodatours.entities.enums.TourStatus;

/**
 * Las relaciones se reciben ya resueltas: el mapper no consulta repositorios.
 */
public final class TourMapper {

    private TourMapper() {
    }

    public static Tour toEntity(CreateTourRequest dto, Agency agencia, ExperienceType tipoExperiencia) {
        if (dto == null) {
            return null;
        }
        return Tour.builder()
                .agencia(agencia)
                .tipoExperiencia(tipoExperiencia)
                .titulo(dto.titulo())
                .descripcion(dto.descripcion())
                .destino(dto.destino())
                .precio(dto.precio())
                .duracionHoras(dto.duracionHoras())
                .nivelDificultad(dto.nivelDificultad())
                .serviciosIncluidos(dto.serviciosIncluidos())
                .estado(dto.estado() == null ? TourStatus.ACTIVE : dto.estado())
                .build();
    }

    public static TourResponse toResponse(Tour entity) {
        if (entity == null) {
            return null;
        }
        return new TourResponse(
                entity.getId(),
                entity.getAgencia() == null ? null : entity.getAgencia().getId(),
                entity.getTipoExperiencia() == null ? null : entity.getTipoExperiencia().getId(),
                entity.getTitulo(),
                entity.getDescripcion(),
                entity.getDestino(),
                entity.getPrecio(),
                entity.getDuracionHoras(),
                entity.getNivelDificultad(),
                entity.getServiciosIncluidos(),
                entity.getEstado()
        );
    }

    public static void applyUpdate(UpdateTourRequest dto, Tour entity,
                                   Agency agencia, ExperienceType tipoExperiencia) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setAgencia(agencia);
        entity.setTipoExperiencia(tipoExperiencia);
        entity.setTitulo(dto.titulo());
        entity.setDescripcion(dto.descripcion());
        entity.setDestino(dto.destino());
        entity.setPrecio(dto.precio());
        entity.setDuracionHoras(dto.duracionHoras());
        entity.setNivelDificultad(dto.nivelDificultad());
        entity.setServiciosIncluidos(dto.serviciosIncluidos());
        if (dto.estado() != null) {
            entity.setEstado(dto.estado());
        }
    }
}
