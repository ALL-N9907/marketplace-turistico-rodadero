package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.api.dtos.request.CreateTourScheduleRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTourScheduleRequest;
import com.edu.unimag.rodatours.api.dtos.response.TourScheduleResponse;
import com.edu.unimag.rodatours.services.TourScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TourScheduleController {
    private final TourScheduleService tourScheduleService;

    @PostMapping("/api/tours/{tourId}/schedules")
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.tour(#tourId)")
    public ResponseEntity<TourScheduleResponse> create(@PathVariable @Positive Long tourId,
                                                       @Valid @RequestBody CreateTourScheduleRequest request) {
        TourScheduleResponse response = tourScheduleService.create(tourId, request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/schedules/{id}").buildAndExpand(response.id()).toUri()).body(response);
    }

    @GetMapping("/api/tours/{tourId}/schedules")
    public List<TourScheduleResponse> getByTour(@PathVariable @Positive Long tourId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return tourScheduleService.getByTourId(tourId, fecha);
    }

    @GetMapping("/api/schedules/{id}")
    public TourScheduleResponse getById(@PathVariable @Positive Long id) {
        return tourScheduleService.getById(id);
    }

    @PutMapping("/api/schedules/{id}/guide")
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.schedule(#id)")
    public TourScheduleResponse assignGuide(@PathVariable @Positive Long id,
                                             @RequestParam @Positive Long guiaId) {
        return tourScheduleService.assignGuide(id, guiaId);
    }

    @PutMapping("/api/schedules/{id}")
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.schedule(#id)")
    public TourScheduleResponse update(@PathVariable @Positive Long id,
                                      @Valid @RequestBody UpdateTourScheduleRequest request) {
        return tourScheduleService.update(id, request);
    }

    @DeleteMapping("/api/schedules/{id}")
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.schedule(#id)")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        tourScheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
