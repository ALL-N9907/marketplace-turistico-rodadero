package com.edu.unimag.rodatours.dtos.mapper;

import com.edu.unimag.rodatours.dtos.response.AgencyOccupancyResponse;
import com.edu.unimag.rodatours.dtos.response.AgencyProductivityResponse;
import com.edu.unimag.rodatours.dtos.response.NoShowTouristResponse;
import com.edu.unimag.rodatours.repositories.projections.AgencyOccupancyProjection;
import com.edu.unimag.rodatours.repositories.projections.AgencyProductivityProjection;
import com.edu.unimag.rodatours.repositories.projections.NoShowTouristProjection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Transforma las proyecciones agregadas en los DTOs de reporte del documento.
 */
public final class ReportMapper {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private ReportMapper() {
    }

    public static AgencyOccupancyResponse toOccupancyResponse(AgencyOccupancyProjection projection,
                                                              LocalDate desde,
                                                              LocalDate hasta) {
        if (projection == null) {
            return null;
        }
        long totales = projection.getCuposTotales() == null ? 0L : projection.getCuposTotales();
        long ocupados = projection.getCuposOcupados() == null ? 0L : projection.getCuposOcupados();

        return new AgencyOccupancyResponse(
                projection.getAgenciaId(),
                projection.getNombreComercial(),
                desde,
                hasta,
                totales,
                ocupados,
                percentage(ocupados, totales)
        );
    }

    public static AgencyProductivityResponse toProductivityResponse(AgencyProductivityProjection projection,
                                                                    int posicion) {
        if (projection == null) {
            return null;
        }
        return new AgencyProductivityResponse(
                projection.getAgenciaId(),
                projection.getNombreComercial(),
                projection.getToursCompletados(),
                posicion
        );
    }

    public static NoShowTouristResponse toNoShowResponse(NoShowTouristProjection projection,
                                                         LocalDate desde,
                                                         LocalDate hasta) {
        if (projection == null) {
            return null;
        }
        return new NoShowTouristResponse(
                projection.getTuristaId(),
                projection.getNombreCompleto(),
                projection.getCantidadNoShow(),
                desde,
                hasta
        );
    }

    private static BigDecimal percentage(long ocupados, long totales) {
        if (totales <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(ocupados)
                .multiply(CIEN)
                .divide(BigDecimal.valueOf(totales), 2, RoundingMode.HALF_UP);
    }
}
