package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.services.QrCheckInService;
import com.edu.unimag.rodatours.services.ReservationVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class QrCheckInController {
    private final QrCheckInService checkIn;

    @PostMapping("/api/reservations/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENCY_OPERATOR', 'GUIDE')")
    public ResponseEntity<ReservationVoucherService.Voucher> redeem(
            @Valid @RequestBody ReservationVoucherController.CheckInRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(checkIn.checkIn(request.qrPayload()));
    }
}
