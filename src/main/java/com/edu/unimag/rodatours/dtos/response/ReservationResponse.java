package com.edu.unimag.rodatours.dtos.response;

import com.edu.unimag.rodatours.entities.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long turistaId,
        Long horarioId,
        LocalDateTime fechaReserva,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        Integer cantidadPersonas,
        BigDecimal precioTotal,
        ReservationStatus estado,
        String motivoCancelacion,
        String observaciones,
        String codigoQr,
        LocalDateTime fechaModificacion
) {
}
