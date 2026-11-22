package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.api.dtos.request.CreateTourScheduleRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTourScheduleRequest;
import com.edu.unimag.rodatours.api.dtos.response.AvailabilitySlotResponse;
import com.edu.unimag.rodatours.api.dtos.response.TourScheduleResponse;

import java.time.LocalDate;
import java.util.List;

public interface TourScheduleService {

    TourScheduleResponse create(Long tourId, CreateTourScheduleRequest request);

    TourScheduleResponse getById(Long id);

    List<TourScheduleResponse> getByTourId(Long tourId, LocalDate fecha);

    List<AvailabilitySlotResponse> getAvailableSlots(Long tourId, LocalDate fecha, int cantidadPersonas);

    TourScheduleResponse assignGuide(Long scheduleId, Long guiaId);

    TourScheduleResponse update(Long id, UpdateTourScheduleRequest request);

    void delete(Long id);
}
