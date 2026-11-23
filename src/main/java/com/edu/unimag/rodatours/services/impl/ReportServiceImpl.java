package com.edu.unimag.rodatours.services.impl;

import com.edu.unimag.rodatours.api.dtos.response.AgencyOccupancyResponse;
import com.edu.unimag.rodatours.api.dtos.response.AgencyProductivityResponse;
import com.edu.unimag.rodatours.api.dtos.response.NoShowTouristResponse;
import com.edu.unimag.rodatours.services.ReportService;
import com.edu.unimag.rodatours.services.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReservationService reservationService;

    @Override
    public AgencyOccupancyResponse getAgencyOccupancy(Long agenciaId, LocalDate desde, LocalDate hasta) {
        return reservationService.getAgencyOccupancy(agenciaId, desde, hasta);
    }

    @Override
    public List<AgencyProductivityResponse> getAgencyProductivityRanking(LocalDate desde, LocalDate hasta) {
        return reservationService.getAgencyProductivityRanking(desde, hasta);
    }

    @Override
    public List<NoShowTouristResponse> getNoShowTouristsReport(LocalDate desde, LocalDate hasta) {
        return reservationService.getNoShowTouristsReport(desde, hasta);
    }
}
