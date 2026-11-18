package com.edu.unimag.rodatours.dtos.mapper;

import com.edu.unimag.rodatours.dtos.request.CreateReviewRequest;
import com.edu.unimag.rodatours.dtos.request.UpdateReviewRequest;
import com.edu.unimag.rodatours.dtos.response.ReviewResponse;
import com.edu.unimag.rodatours.entities.Reservation;
import com.edu.unimag.rodatours.entities.Review;

public final class ReviewMapper {

    private ReviewMapper() {
    }

    public static Review toEntity(CreateReviewRequest dto, Reservation reserva) {
        if (dto == null) {
            return null;
        }
        return Review.builder()
                .reserva(reserva)
                .calificacion(dto.calificacion())
                .comentario(dto.comentario())
                .build();
    }

    public static ReviewResponse toResponse(Review entity) {
        if (entity == null) {
            return null;
        }
        return new ReviewResponse(
                entity.getId(),
                entity.getReserva() == null ? null : entity.getReserva().getId(),
                entity.getCalificacion(),
                entity.getComentario(),
                entity.getFechaCreacion()
        );
    }

    public static void applyUpdate(UpdateReviewRequest dto, Review entity) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setCalificacion(dto.calificacion());
        entity.setComentario(dto.comentario());
    }
}
