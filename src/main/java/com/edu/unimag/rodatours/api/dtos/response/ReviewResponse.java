package com.edu.unimag.rodatours.api.dtos.response;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long reservaId,
        Integer calificacion,
        String comentario,
        LocalDateTime fechaCreacion
) {
}
