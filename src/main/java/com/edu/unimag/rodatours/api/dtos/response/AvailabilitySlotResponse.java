package com.edu.unimag.rodatours.api.dtos.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Franja horaria con capacidad real para un tour en una fecha concreta.
 */
public record AvailabilitySlotResponse(
        Long horarioId,
        Long tourId,
        LocalDate fechaTour,
        LocalTime horaInicio,
        LocalDateTime fechaFin,
        Integer cuposTotales,
        Integer cuposDisponibles
) {
}
