package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.api.dtos.request.CreateTouristRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTouristRequest;
import com.edu.unimag.rodatours.api.dtos.response.TouristResponse;
import com.edu.unimag.rodatours.domain.entities.enums.TouristStatus;
import com.edu.unimag.rodatours.services.TouristService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/tourists")
@RequiredArgsConstructor
public class TouristController {
    private final TouristService touristService;

    @PostMapping
    public ResponseEntity<TouristResponse> create(@Valid @RequestBody CreateTouristRequest request) {
        TouristResponse response = touristService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri()).body(response);
    }

    @GetMapping("/{id}")
    public TouristResponse getById(@PathVariable @Positive Long id) {
        return touristService.getById(id);
    }

    @GetMapping
    public List<TouristResponse> getAll(@RequestParam(required = false) TouristStatus estado) {
        return estado == null ? touristService.getAll() : touristService.getByStatus(estado);
    }

    @PutMapping("/{id}")
    public TouristResponse update(@PathVariable @Positive Long id,
                                  @Valid @RequestBody UpdateTouristRequest request) {
        return touristService.update(id, request);
    }
}
