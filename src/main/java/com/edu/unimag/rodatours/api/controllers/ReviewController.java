package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.api.dtos.request.CreateReviewRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateReviewRequest;
import com.edu.unimag.rodatours.api.dtos.response.ReviewResponse;
import com.edu.unimag.rodatours.security.AccessPolicy;
import com.edu.unimag.rodatours.services.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviews;
    private final AccessPolicy access;

    @PostMapping
    @PreAuthorize("hasRole('TOURIST') and @accessPolicy.touristReservation(#request.reservaId())")
    public ResponseEntity<ReviewResponse> create(@Valid @RequestBody CreateReviewRequest request) {
        var review = reviews.create(access.currentTouristId(), request);
        return ResponseEntity.created(URI.create("/api/reviews/" + review.id())).body(review);
    }

    @GetMapping("/{id}")
    public ReviewResponse get(@PathVariable @Positive Long id) {
        return reviews.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('TOURIST')")
    public List<ReviewResponse> mine() {
        return reviews.getByTouristId(access.currentTouristId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TOURIST') and @accessPolicy.review(#id)")
    public ReviewResponse update(@PathVariable @Positive Long id, @Valid @RequestBody UpdateReviewRequest request) {
        return reviews.update(id, access.currentTouristId(), request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TOURIST') and @accessPolicy.review(#id)")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        reviews.delete(id, access.currentTouristId());
        return ResponseEntity.noContent().build();
    }
}
