package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.api.dtos.response.AvailabilitySlotResponse;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityService {
    List<AvailabilitySlotResponse> getAvailableSlots(Long tourId, LocalDate fecha, int cantidadPersonas);
}
