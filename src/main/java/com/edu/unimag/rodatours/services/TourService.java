package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.api.dtos.request.CreateTourRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTourRequest;
import com.edu.unimag.rodatours.api.dtos.response.TourResponse;
import com.edu.unimag.rodatours.domain.entities.enums.DifficultyLevel;

import java.time.LocalDate;
import java.util.List;

public interface TourService {

    TourResponse create(CreateTourRequest request);

    TourResponse getById(Long id);

    List<TourResponse> getAll();

    List<TourResponse> getByAgency(Long agenciaId, DifficultyLevel dificultad, String destino);

    List<TourResponse> findAvailableByDestinationAndDate(String destino, LocalDate fecha, int cantidadPersonas);

    TourResponse update(Long id, UpdateTourRequest request);

    void delete(Long id);
}
