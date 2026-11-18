package com.edu.unimag.rodatours.dtos.mapper;

import com.edu.unimag.rodatours.dtos.response.AvailabilitySlotResponse;
import com.edu.unimag.rodatours.entities.TourSchedule;

/**
 * Convierte salidas con cupo real en franjas de disponibilidad para el catálogo.
 */
public final class AvailabilityMapper {

    private AvailabilityMapper() {
    }

    public static AvailabilitySlotResponse toResponse(TourSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        return new AvailabilitySlotResponse(
                schedule.getId(),
                schedule.getTour() == null ? null : schedule.getTour().getId(),
                schedule.getFechaTour(),
                schedule.getHoraInicio(),
                schedule.calculateEndDateTime(),
                schedule.getCuposTotales(),
                schedule.getCuposDisponibles()
        );
    }
}
