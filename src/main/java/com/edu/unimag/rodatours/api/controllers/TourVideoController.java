package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.media.TourVideoService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tours/{tourId}/video")
@RequiredArgsConstructor
public class TourVideoController {
    private final TourVideoService videos;

    @GetMapping
    public TourVideoService.VideoResponse get(@PathVariable @Positive Long tourId) {
        return videos.get(tourId);
    }

    @PostMapping("/upload-authorization")
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.tour(#tourId)")
    public ResponseEntity<TourVideoService.UploadResponse> begin(@PathVariable @Positive Long tourId) {
        return ResponseEntity.status(201).body(videos.begin(tourId));
    }

    @PostMapping("/complete")
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.tour(#tourId)")
    public TourVideoService.VideoResponse complete(@PathVariable @Positive Long tourId) {
        return videos.complete(tourId);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.tour(#tourId)")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long tourId) {
        videos.delete(tourId);
        return ResponseEntity.noContent().build();
    }
}
