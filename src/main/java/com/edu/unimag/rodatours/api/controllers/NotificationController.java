package com.edu.unimag.rodatours.api.controllers;

import com.edu.unimag.rodatours.notifications.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notifications;

    @GetMapping
    public NotificationPage mine(@RequestParam(defaultValue = "0") @Min(0) int page,
                                 @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = notifications.mine(page, size);
        return new NotificationPage(result.getContent(), result.getTotalElements(), result.getNumber(), result.getSize());
    }

    @PutMapping("/{id}/read")
    public NotificationService.NotificationResponse read(@PathVariable @Positive Long id) {
        return notifications.read(id);
    }

    public record NotificationPage(List<NotificationService.NotificationResponse> content,
                                   long totalElements, int page, int size) {}
}
