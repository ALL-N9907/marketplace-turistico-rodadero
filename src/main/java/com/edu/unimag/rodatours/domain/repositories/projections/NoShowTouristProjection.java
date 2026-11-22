package com.edu.unimag.rodatours.domain.repositories.projections;

/**
 * Fila del reporte de turistas con más inasistencias.
 */
public interface NoShowTouristProjection {

    Long getTuristaId();

    String getNombreCompleto();

    Long getCantidadNoShow();
}
