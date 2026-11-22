package com.edu.unimag.rodatours.api.dtos.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TourScheduleResponse(
        Long id,
        Long tourId,
        Long guiaId,
        LocalDate fechaTour,
        LocalTime horaInicio,
        LocalDateTime fechaFin,
        Integer cuposTotales,
        Integer cuposDisponibles
) {
}
