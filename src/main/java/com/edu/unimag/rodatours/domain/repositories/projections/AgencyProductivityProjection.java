package com.edu.unimag.rodatours.domain.repositories.projections;

/**
 * Fila del ranking de agencias por tours completados.
 */
public interface AgencyProductivityProjection {

    Long getAgenciaId();

    String getNombreComercial();

    Long getToursCompletados();
}
