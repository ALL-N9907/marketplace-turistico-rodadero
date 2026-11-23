package com.edu.unimag.rodatours.services.impl;

import com.edu.unimag.rodatours.api.dtos.response.AvailabilitySlotResponse;
import com.edu.unimag.rodatours.services.AvailabilityService;
import com.edu.unimag.rodatours.services.TourScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {
    private final TourScheduleService tourScheduleService;

    @Override
    public List<AvailabilitySlotResponse> getAvailableSlots(Long tourId, LocalDate fecha, int cantidadPersonas) {
        return tourScheduleService.getAvailableSlots(tourId, fecha, cantidadPersonas);
    }
}
