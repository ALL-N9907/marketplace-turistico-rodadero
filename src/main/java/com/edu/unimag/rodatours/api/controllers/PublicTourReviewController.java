package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.services.PublicTourReviewService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PublicTourReviewController {
    private final PublicTourReviewService reviews;

    @GetMapping("/api/tours/{id}/reviews")
    public PublicTourReviewService.ReviewPage list(@PathVariable @Positive Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(100) int size) {
        return reviews.list(id, page, size);
    }
}
