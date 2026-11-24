package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.history.ReservationHistoryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReservationHistoryController {
    private final ReservationHistoryService history;

    @GetMapping("/api/reservations/{id}/history")
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.readReservation(#id)")
    public ResponseEntity<ReservationHistoryService.HistoryPage> list(@PathVariable @Positive Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(history.list(id, page, size));
    }
}
