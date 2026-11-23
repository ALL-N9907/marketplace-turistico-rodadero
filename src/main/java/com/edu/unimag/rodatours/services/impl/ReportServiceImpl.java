package com.edu.unimag.rodatours.services.impl;

import com.edu.unimag.rodatours.api.dtos.response.AgencyOccupancyResponse;
import com.edu.unimag.rodatours.api.dtos.response.AgencyProductivityResponse;
import com.edu.unimag.rodatours.api.dtos.response.NoShowTouristResponse;
import com.edu.unimag.rodatours.api.dtos.response.ExperienceTypeCancellationResponse;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import com.edu.unimag.rodatours.services.ReportService;
import com.edu.unimag.rodatours.services.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReservationService reservationService;
    private final ReservationRepository reservations;

    @Override
    @Transactional(readOnly = true)
    public List<ExperienceTypeCancellationResponse> getExperienceTypeCancellations(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new ValidationException("El rango de fechas es inválido.");
        }
        return reservations.countByExperienceTypeAndState(List.of(
                ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW), desde, hasta)
                .stream().map(row -> new ExperienceTypeCancellationResponse(
                        row.getTipoExperiencia(), row.getEstado(), row.getCantidad())).toList();
    }

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
