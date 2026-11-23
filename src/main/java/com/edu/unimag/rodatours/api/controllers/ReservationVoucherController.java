package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.services.ReservationVoucherService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations/{id}")
@RequiredArgsConstructor
public class ReservationVoucherController {
    private final ReservationVoucherService vouchers;

    @GetMapping("/voucher")
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.readReservation(#id)")
    public ResponseEntity<ReservationVoucherService.Voucher> voucher(@PathVariable @Positive Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(vouchers.get(id));
    }

    @GetMapping(value = "/voucher/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.readReservation(#id)")
    public ResponseEntity<byte[]> qr(@PathVariable @Positive Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("Content-Disposition", "attachment; filename=reservation-" + id + "-qr.png")
                .body(vouchers.qr(id));
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasRole('ADMIN') or @accessPolicy.operateReservation(#id) or @accessPolicy.completeReservation(#id)")
    public ReservationVoucherService.Voucher checkIn(@PathVariable @Positive Long id,
                                                    @Valid @RequestBody CheckInRequest request) {
        return vouchers.checkIn(id, request.qrPayload());
    }

    public record CheckInRequest(@NotBlank @Size(max = 150) String qrPayload) {}
}
