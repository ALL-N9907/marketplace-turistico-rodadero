package com.edu.unimag.rodatours.dtos.request;

/**
 * Las reservas solo permiten actualizar sus notas; el resto de campos son
 * calculados por el servidor o se cambian mediante endpoints de estado.
 */
public record UpdateReservationRequest(String observaciones) {
}
