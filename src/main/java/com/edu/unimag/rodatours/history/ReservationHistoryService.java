package com.edu.unimag.rodatours.history;

import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
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
public class ReservationHistoryService {
    private final ReservationHistoryRepository history;
    private final ReservationRepository reservations;

    @Transactional(readOnly = true)
    public HistoryPage list(Long reservationId, int page, int size) {
        if (!reservations.existsById(reservationId)) throw new ResourceNotFoundException("Reserva no encontrada.");
        var result = history.findByReservationId(reservationId, PageRequest.of(page, size,
                Sort.by(Sort.Order.asc("occurredAt"), Sort.Order.asc("id"))));
        var content = result.map(e -> new HistoryItem(e.getId(), e.getPreviousStatus(), e.getStatus(),
                e.getOccurredAt(), e.getActorRole(), e.getReason())).getContent();
        return new HistoryPage(content, result.getTotalElements(), page, size, result.getTotalPages());
    }

    public record HistoryItem(Long id, ReservationStatus previousStatus, ReservationStatus status,
                              LocalDateTime occurredAt, String actorRole, String reason) {}
    public record HistoryPage(List<HistoryItem> content, long totalElements, int page, int size, int totalPages) {}
}
