package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReviewRepository;
import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicTourReviewService {
    private final TourRepository tours;
    private final ReviewRepository reviews;

    @Transactional(readOnly = true)
    public ReviewPage list(Long tourId, int page, int size) {
        tours.findById(tourId).filter(t -> t.isActive() && t.getAgencia().isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Tour no disponible."));
        var result = reviews.findByReservaHorarioTourIdAndReservaEstadoAndPublishedTrue(tourId, ReservationStatus.COMPLETED,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("id"))));
        var content = result.map(r -> new PublicReview(r.getId(), r.getCalificacion(),
                r.getComentario(), r.getFechaCreacion())).getContent();
        return new ReviewPage(content, result.getTotalElements(), page, size, result.getTotalPages(),
                result.getTotalElements() == 0 ? null : reviews.averageByTour(tourId, ReservationStatus.COMPLETED));
    }

    // No expone IDs de reservas/turistas, nombres, correo, teléfono ni QR.
    public record PublicReview(Long id, Integer rating, String comment, LocalDateTime createdAt) {}
    public record ReviewPage(List<PublicReview> content, long totalElements, int page, int size,
                             int totalPages, Double averageRating) {}
}
