package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.services.AgencyNearbyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor
public class AgencyNearbyController {
    private final AgencyNearbyService nearby;
    // POST de sólo lectura para no poner coordenadas del visitante en URLs/historial.
    @PostMapping("/api/agencies/nearby")
    public List<AgencyNearbyService.Nearby> search(@Valid @RequestBody Request request){return nearby.search(request.latitude(),request.longitude(),request.radiusKm());}
    public record Request(@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
                          @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
                          @NotNull @DecimalMin("0.1") @DecimalMax("200") Double radiusKm){}
}
