package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.domain.repositories.ReviewRepository;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class ReviewModerationController {
    private final ReviewRepository reviews;
    @GetMapping
    @Transactional(readOnly = true)
    public List<Item> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                           @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reviews.findAll(PageRequest.of(page, size, Sort.by("id").descending())).map(r ->
                new Item(r.getId(), r.getCalificacion(), r.getComentario(), r.isPublished(), r.getModerationReason())).getContent();
    }
    @PutMapping("/{id}/moderation")
    @Transactional
    public Item moderate(@PathVariable @Positive Long id, @Valid @RequestBody Moderation body) {
        var r = reviews.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada."));
        r.setPublished(body.published()); r.setModerationReason(body.reason().trim()); reviews.save(r);
        return new Item(r.getId(), r.getCalificacion(), r.getComentario(), r.isPublished(), r.getModerationReason());
    }
    public record Moderation(boolean published, @NotBlank @Size(max = 1000) String reason) {}
    public record Item(Long id, Integer rating, String comment, boolean published, String reason) {}
}
