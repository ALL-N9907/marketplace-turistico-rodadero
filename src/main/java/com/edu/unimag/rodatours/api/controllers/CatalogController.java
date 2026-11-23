package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.services.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService catalog;

    @GetMapping("/api/catalog/tours")
    public CatalogService.SearchResponse search(@Valid @ModelAttribute CatalogService.Filter filter,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return catalog.search(filter, page, size);
    }

    @GetMapping("/api/catalog/tours/compare")
    public List<CatalogService.CatalogEntry> compare(@RequestParam @Size(min = 2, max = 6) List<@Positive Long> ids) {
        return catalog.compare(ids);
    }

    @GetMapping("/api/tours/{id}/details")
    public CatalogService.TourDetails details(@PathVariable @Positive Long id) { return catalog.details(id); }

    @PutMapping("/api/tours/{id}/details")
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.tour(#id)")
    public CatalogService.TourDetails updateDetails(@PathVariable @Positive Long id,
            @Valid @RequestBody CatalogService.TourDetails request) { return catalog.updateDetails(id, request); }

    @GetMapping("/api/agencies/{id}/location")
    public CatalogService.AgencyLocation location(@PathVariable @Positive Long id) { return catalog.location(id); }

    @PutMapping("/api/agencies/{id}/location")
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.agency(#id)")
    public CatalogService.AgencyLocation updateLocation(@PathVariable @Positive Long id,
            @Valid @RequestBody CatalogService.AgencyLocation request) { return catalog.updateLocation(id, request); }
}
