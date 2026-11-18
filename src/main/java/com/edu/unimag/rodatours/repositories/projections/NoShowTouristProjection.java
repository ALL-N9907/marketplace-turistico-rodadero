package com.edu.unimag.rodatours.repositories.projections;

/**
 * Fila del reporte de turistas con más inasistencias.
 */
public interface NoShowTouristProjection {

    Long getTuristaId();

    String getNombreCompleto();

    Long getCantidadNoShow();
}
