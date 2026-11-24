package com.edu.unimag.rodatours.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationHistoryRepository extends JpaRepository<ReservationHistoryEntry, Long> {
    Page<ReservationHistoryEntry> findByReservationId(Long reservationId, Pageable pageable);
}
