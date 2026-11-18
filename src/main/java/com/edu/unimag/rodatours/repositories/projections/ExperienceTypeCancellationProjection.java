package com.edu.unimag.rodatours.repositories.projections;

import com.edu.unimag.rodatours.entities.enums.ReservationStatus;

/**
 * Conteo de cancelaciones e inasistencias agrupado por tipo de experiencia y estado.
 */
public interface ExperienceTypeCancellationProjection {

    String getTipoExperiencia();

    ReservationStatus getEstado();

    Long getCantidad();
}
