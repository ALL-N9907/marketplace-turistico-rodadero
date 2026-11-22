package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.api.dtos.request.CancelReservationRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateReservationRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateReservationRequest;
import com.edu.unimag.rodatours.api.dtos.response.AgencyOccupancyResponse;
import com.edu.unimag.rodatours.api.dtos.response.AgencyProductivityResponse;
import com.edu.unimag.rodatours.api.dtos.response.NoShowTouristResponse;
import com.edu.unimag.rodatours.api.dtos.response.ReservationResponse;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationService {

    ReservationResponse create(CreateReservationRequest request);

    ReservationResponse getById(Long id);

    ReservationResponse getByQrCode(String codigoQr);

    List<ReservationResponse> getAll();

    List<ReservationResponse> getByTourist(Long turistaId, ReservationStatus estado);

    List<ReservationResponse> getBySchedule(Long horarioId);

    List<ReservationResponse> getByReservationDateRange(LocalDateTime desde, LocalDateTime hasta);

    ReservationResponse updateNotes(Long id, UpdateReservationRequest request);

    ReservationResponse confirm(Long id);

    ReservationResponse cancel(Long id, CancelReservationRequest request);

    ReservationResponse complete(Long id, String observacionesAdicionales);

    ReservationResponse markNoShow(Long id);

    AgencyOccupancyResponse getAgencyOccupancy(Long agenciaId, LocalDate desde, LocalDate hasta);

    List<AgencyProductivityResponse> getAgencyProductivityRanking(LocalDate desde, LocalDate hasta);

    List<NoShowTouristResponse> getNoShowTouristsReport(LocalDate desde, LocalDate hasta);
}
