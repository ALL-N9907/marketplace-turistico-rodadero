package com.edu.unimag.rodatours.services.mapper;

import com.edu.unimag.rodatours.api.dtos.request.CreateReservationRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateReservationRequest;
import com.edu.unimag.rodatours.api.dtos.response.ReservationResponse;
import com.edu.unimag.rodatours.domain.entities.Reservation;
import com.edu.unimag.rodatours.domain.entities.TourSchedule;
import com.edu.unimag.rodatours.domain.entities.Tourist;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fechas de fin, precio total y código QR los calcula el servicio; el mapper
 * solo los recibe. {@code fechaReserva} y {@code fechaModificacion} los asigna
 * la entidad en sus callbacks de persistencia.
 */
public final class ReservationMapper {

    private ReservationMapper() {
    }

    public static Reservation toEntity(CreateReservationRequest dto,
                                       Tourist turista,
                                       TourSchedule horario,
                                       LocalDateTime fechaInicio,
                                       LocalDateTime fechaFin,
                                       BigDecimal precioTotal,
                                       String codigoQr) {
        if (dto == null) {
            return null;
        }
        return Reservation.builder()
                .turista(turista)
                .horario(horario)
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .cantidadPersonas(dto.cantidadPersonas())
                .precioTotal(precioTotal)
                .codigoQr(codigoQr)
                .observaciones(dto.observaciones())
                .build();
    }

    public static ReservationResponse toResponse(Reservation entity) {
        if (entity == null) {
            return null;
        }
        return new ReservationResponse(
                entity.getId(),
                entity.getTurista() == null ? null : entity.getTurista().getId(),
                entity.getHorario() == null ? null : entity.getHorario().getId(),
                entity.getFechaReserva(),
                entity.getFechaInicio(),
                entity.getFechaFin(),
                entity.getCantidadPersonas(),
                entity.getPrecioTotal(),
                entity.getEstado(),
                entity.getMotivoCancelacion(),
                entity.getObservaciones(),
                entity.getCodigoQr(),
                entity.getFechaModificacion()
        );
    }

    public static void applyUpdate(UpdateReservationRequest dto, Reservation entity) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setObservaciones(dto.observaciones());
    }
}
