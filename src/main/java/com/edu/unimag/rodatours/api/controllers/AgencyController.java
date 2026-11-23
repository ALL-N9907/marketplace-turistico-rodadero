package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.api.dtos.request.CreateAgencyRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateAgencyRequest;
import com.edu.unimag.rodatours.api.dtos.response.AgencyResponse;
import com.edu.unimag.rodatours.services.AgencyService;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/agencies")
@RequiredArgsConstructor
public class AgencyController {
    private final AgencyService agencyService;

    @PostMapping
    public ResponseEntity<AgencyResponse> create(@Valid @RequestBody CreateAgencyRequest request) {
        AgencyResponse response = agencyService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri()).body(response);
    }

    @GetMapping("/{id}")
    public AgencyResponse getById(@PathVariable @Positive Long id) {
        return agencyService.getById(id);
    }

    @GetMapping
    public List<AgencyResponse> getAll(@RequestParam(required = false) String query,
                                       @RequestParam(defaultValue = "false") boolean activeOnly) {
        if (query != null) {
            if (query.isBlank()) {
                throw new ValidationException("El criterio de búsqueda no puede estar vacío.");
            }
            return agencyService.searchActiveByNameOrLocation(query);
        }
        return activeOnly ? agencyService.getAllActive() : agencyService.getAll();
    }

    @PutMapping("/{id}")
    public AgencyResponse update(@PathVariable @Positive Long id,
                                 @Valid @RequestBody UpdateAgencyRequest request) {
        return agencyService.update(id, request);
    }
}
