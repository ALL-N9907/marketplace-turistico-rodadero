package com.edu.unimag.rodatours.services.mapper;

import com.edu.unimag.rodatours.api.dtos.request.CreateTourScheduleRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTourScheduleRequest;
import com.edu.unimag.rodatours.api.dtos.response.TourScheduleResponse;
import com.edu.unimag.rodatours.domain.entities.Guide;
import com.edu.unimag.rodatours.domain.entities.Tour;
import com.edu.unimag.rodatours.domain.entities.TourSchedule;

/**
 * Los cupos disponibles se inicializan desde el total, pero nunca se recalculan
 * en una actualización: su control es responsabilidad del servicio.
 */
public final class TourScheduleMapper {

    private TourScheduleMapper() {
    }

    public static TourSchedule toEntity(CreateTourScheduleRequest dto, Tour tour, Guide guia) {
        if (dto == null) {
            return null;
        }
        return TourSchedule.builder()
                .tour(tour)
                .guia(guia)
                .fechaTour(dto.fechaTour())
                .horaInicio(dto.horaInicio())
                .cuposTotales(dto.cuposTotales())
                .cuposDisponibles(dto.cuposTotales())
                .build();
    }

    public static TourScheduleResponse toResponse(TourSchedule entity) {
        if (entity == null) {
            return null;
        }
        return new TourScheduleResponse(
                entity.getId(),
                entity.getTour() == null ? null : entity.getTour().getId(),
                entity.getGuia() == null ? null : entity.getGuia().getId(),
                entity.getFechaTour(),
                entity.getHoraInicio(),
                entity.calculateEndDateTime(),
                entity.getCuposTotales(),
                entity.getCuposDisponibles()
        );
    }

    public static void applyUpdate(UpdateTourScheduleRequest dto, TourSchedule entity, Guide guia) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setGuia(guia);
        entity.setFechaTour(dto.fechaTour());
        entity.setHoraInicio(dto.horaInicio());
        entity.setCuposTotales(dto.cuposTotales());
    }
}
