package com.edu.unimag.rodatours.api.dtos.response;

import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;

public record ExperienceTypeCancellationResponse(String tipoExperiencia, ReservationStatus estado, Long cantidad) {}
