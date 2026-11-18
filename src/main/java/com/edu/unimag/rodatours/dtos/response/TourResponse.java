package com.edu.unimag.rodatours.dtos.response;

import com.edu.unimag.rodatours.entities.enums.DifficultyLevel;
import com.edu.unimag.rodatours.entities.enums.TourStatus;

import java.math.BigDecimal;

public record TourResponse(
        Long id,
        Long agenciaId,
        Long tipoExperienciaId,
        String titulo,
        String descripcion,
        String destino,
        BigDecimal precio,
        BigDecimal duracionHoras,
        DifficultyLevel nivelDificultad,
        String serviciosIncluidos,
        TourStatus estado
) {
}
