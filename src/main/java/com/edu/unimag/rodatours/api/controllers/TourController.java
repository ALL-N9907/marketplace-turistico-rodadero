package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.api.dtos.request.CreateTourRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTourRequest;
import com.edu.unimag.rodatours.api.dtos.response.TourResponse;
import com.edu.unimag.rodatours.domain.entities.enums.DifficultyLevel;
import com.edu.unimag.rodatours.services.TourService;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
public class TourController {
    private final TourService tourService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.agency(#request.agenciaId())")
    public ResponseEntity<TourResponse> create(@Valid @RequestBody CreateTourRequest request) {
        TourResponse response = tourService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri()).body(response);
    }

    @GetMapping
    public List<TourResponse> getAll(@RequestParam(required = false) @Positive Long agenciaId,
                                     @RequestParam(required = false) DifficultyLevel dificultad,
                                     @RequestParam(required = false) String destino) {
        if (agenciaId == null && (dificultad != null || destino != null)) {
            throw new ValidationException("Los filtros de dificultad y destino requieren agenciaId.");
        }
        return agenciaId == null ? tourService.getAll() : tourService.getByAgency(agenciaId, dificultad, destino);
    }

    @GetMapping("/{id}")
    public TourResponse getById(@PathVariable @Positive Long id) {
        return tourService.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (@accessPolicy.tour(#id) and @accessPolicy.agency(#request.agenciaId()))")
    public TourResponse update(@PathVariable @Positive Long id, @Valid @RequestBody UpdateTourRequest request) {
        return tourService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.tour(#id)")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        tourService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
