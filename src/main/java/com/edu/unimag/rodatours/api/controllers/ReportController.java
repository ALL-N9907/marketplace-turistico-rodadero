package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.api.dtos.response.AgencyOccupancyResponse;
import com.edu.unimag.rodatours.api.dtos.response.AgencyProductivityResponse;
import com.edu.unimag.rodatours.api.dtos.response.NoShowTouristResponse;
import com.edu.unimag.rodatours.services.ReportService;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/agency-occupancy")
    public AgencyOccupancyResponse getAgencyOccupancy(@RequestParam @Positive Long agenciaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        validateRange(desde, hasta);
        return reportService.getAgencyOccupancy(agenciaId, desde, hasta);
    }

    @GetMapping("/agency-productivity")
    public List<AgencyProductivityResponse> getAgencyProductivity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        validateRange(desde, hasta);
        return reportService.getAgencyProductivityRanking(desde, hasta);
    }

    @GetMapping("/no-show-tourists")
    public List<NoShowTouristResponse> getNoShowTourists(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        validateRange(desde, hasta);
        return reportService.getNoShowTouristsReport(desde, hasta);
    }

    private void validateRange(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new ValidationException("La fecha desde no puede ser posterior a hasta.");
        }
    }
}
