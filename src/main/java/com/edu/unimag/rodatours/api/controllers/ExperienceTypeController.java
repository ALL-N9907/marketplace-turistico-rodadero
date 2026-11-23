package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.api.dtos.request.CreateExperienceTypeRequest;
import com.edu.unimag.rodatours.api.dtos.response.ExperienceTypeResponse;
import com.edu.unimag.rodatours.services.ExperienceTypeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/experience-types")
@RequiredArgsConstructor
public class ExperienceTypeController {
    private final ExperienceTypeService experienceTypeService;

    @PostMapping
    public ResponseEntity<ExperienceTypeResponse> create(@Valid @RequestBody CreateExperienceTypeRequest request) {
        ExperienceTypeResponse response = experienceTypeService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri()).body(response);
    }

    @GetMapping
    public List<ExperienceTypeResponse> getAll() {
        return experienceTypeService.getAll();
    }

    @GetMapping("/{id}")
    public ExperienceTypeResponse getById(@PathVariable @Positive Long id) {
        return experienceTypeService.getById(id);
    }
}
