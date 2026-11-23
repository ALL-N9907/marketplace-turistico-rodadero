package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.api.dtos.response.AgencyOccupancyResponse;
import com.edu.unimag.rodatours.api.dtos.response.AgencyProductivityResponse;
import com.edu.unimag.rodatours.api.dtos.response.NoShowTouristResponse;
import com.edu.unimag.rodatours.api.dtos.response.ExperienceTypeCancellationResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    List<ExperienceTypeCancellationResponse> getExperienceTypeCancellations(LocalDate desde, LocalDate hasta);
    AgencyOccupancyResponse getAgencyOccupancy(Long agenciaId, LocalDate desde, LocalDate hasta);

    List<AgencyProductivityResponse> getAgencyProductivityRanking(LocalDate desde, LocalDate hasta);

    List<NoShowTouristResponse> getNoShowTouristsReport(LocalDate desde, LocalDate hasta);
}
