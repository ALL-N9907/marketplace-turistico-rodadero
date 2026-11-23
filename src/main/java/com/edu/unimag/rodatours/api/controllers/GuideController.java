package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.api.dtos.request.CreateGuideRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateGuideRequest;
import com.edu.unimag.rodatours.api.dtos.response.GuideResponse;
import com.edu.unimag.rodatours.domain.entities.enums.GuideStatus;
import com.edu.unimag.rodatours.services.GuideService;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/guides")
@RequiredArgsConstructor
public class GuideController {
    private final GuideService guideService;

    @PostMapping
    public ResponseEntity<GuideResponse> create(@Valid @RequestBody CreateGuideRequest request) {
        GuideResponse response = guideService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri()).body(response);
    }

    @GetMapping("/{id}")
    public GuideResponse getById(@PathVariable @Positive Long id) {
        return guideService.getById(id);
    }

    @GetMapping
    public List<GuideResponse> getAll(@RequestParam(required = false) @Positive Long agenciaId,
                                      @RequestParam(required = false) GuideStatus estado) {
        if ((agenciaId == null) != (estado == null)) {
            throw new ValidationException("Los filtros agenciaId y estado deben proporcionarse juntos.");
        }
        return agenciaId == null ? guideService.getAll() : guideService.getByAgencyAndStatus(agenciaId, estado);
    }

    @PutMapping("/{id}")
    public GuideResponse update(@PathVariable @Positive Long id, @Valid @RequestBody UpdateGuideRequest request) {
        return guideService.update(id, request);
    }
}
