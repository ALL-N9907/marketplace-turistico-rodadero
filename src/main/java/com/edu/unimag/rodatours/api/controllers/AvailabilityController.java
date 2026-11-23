package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.api.dtos.response.AvailabilitySlotResponse;
import com.edu.unimag.rodatours.services.AvailabilityService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {
    private final AvailabilityService availabilityService;

    @GetMapping("/tours/{tourId}")
    public List<AvailabilitySlotResponse> getAvailableSlots(@PathVariable @Positive Long tourId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "1") @Min(1) int cantidadPersonas) {
        return availabilityService.getAvailableSlots(tourId, fecha, cantidadPersonas);
    }
}
