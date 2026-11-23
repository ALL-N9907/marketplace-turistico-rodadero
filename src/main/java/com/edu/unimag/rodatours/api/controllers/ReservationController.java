package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.api.dtos.request.CancelReservationRequest;
import com.edu.unimag.rodatours.api.dtos.request.CompleteReservationRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateReservationRequest;
import com.edu.unimag.rodatours.api.dtos.response.ReservationResponse;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.services.ReservationService;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse response = reservationService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri()).body(response);
    }

    @GetMapping("/{id}")
    public ReservationResponse getById(@PathVariable @Positive Long id) {
        return reservationService.getById(id);
    }

    @GetMapping
    public List<ReservationResponse> getAll(
            @RequestParam(required = false) @Positive Long turistaId,
            @RequestParam(required = false) ReservationStatus estado,
            @RequestParam(required = false) @Positive Long horarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        int filters = (turistaId != null ? 1 : 0) + (horarioId != null ? 1 : 0)
                + (desde != null || hasta != null ? 1 : 0);
        if (filters > 1 || (estado != null && turistaId == null)) {
            throw new ValidationException("Use turistaId (con estado opcional), horarioId o un rango de fechas, sin combinarlos.");
        }
        if (turistaId != null) {
            return reservationService.getByTourist(turistaId, estado);
        }
        if (horarioId != null) {
            return reservationService.getBySchedule(horarioId);
        }
        if (desde != null || hasta != null) {
            if (desde == null || hasta == null || desde.isAfter(hasta)) {
                throw new ValidationException("Debe proporcionar desde y hasta en orden cronológico.");
            }
            return reservationService.getByReservationDateRange(desde, hasta);
        }
        return reservationService.getAll();
    }

    @PutMapping("/{id}/confirm")
    public ReservationResponse confirm(@PathVariable @Positive Long id) {
        return reservationService.confirm(id);
    }

    @PutMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable @Positive Long id,
                                       @Valid @RequestBody CancelReservationRequest request) {
        return reservationService.cancel(id, request);
    }

    @PutMapping("/{id}/complete")
    public ReservationResponse complete(@PathVariable @Positive Long id,
            @Valid @RequestBody(required = false) CompleteReservationRequest request) {
        return reservationService.complete(id, request == null ? null : request.observaciones());
    }

    @PutMapping("/{id}/no-show")
    public ReservationResponse markNoShow(@PathVariable @Positive Long id) {
        return reservationService.markNoShow(id);
    }
}
