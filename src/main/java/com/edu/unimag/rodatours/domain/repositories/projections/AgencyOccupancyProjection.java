package com.edu.unimag.rodatours.domain.repositories.projections;

/**
 * Datos calculados para el reporte de ocupación de una agencia en un rango de fechas.
 */
public interface AgencyOccupancyProjection {

    Long getAgenciaId();

    String getNombreComercial();

    Long getCuposTotales();

    Long getCuposOcupados();
}
