package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.media.TourImageService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/api/tours/{tourId}/images")
public class TourImageController {
    private final TourImageService images;
    @GetMapping public List<TourImageService.Item> list(@PathVariable @Positive Long tourId){return images.list(tourId,false);}
    @GetMapping("/uploads") @PreAuthorize("hasRole('ADMIN') or @accessPolicy.tour(#tourId)")
    public List<TourImageService.Item> manage(@PathVariable @Positive Long tourId){return images.list(tourId,true);}
    @PostMapping @PreAuthorize("hasRole('ADMIN') or @accessPolicy.tour(#tourId)")
    public TourImageService.Upload begin(@PathVariable @Positive Long tourId){return images.begin(tourId);}
    @PostMapping("/{id}/complete") @PreAuthorize("hasRole('ADMIN') or @accessPolicy.tour(#tourId)")
    public TourImageService.Item complete(@PathVariable @Positive Long tourId,@PathVariable @Positive Long id){return images.complete(tourId,id);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN') or @accessPolicy.tour(#tourId)")
    public void delete(@PathVariable @Positive Long tourId,@PathVariable @Positive Long id){images.delete(tourId,id);}
}
