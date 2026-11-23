package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.api.dtos.response.AvailabilitySlotResponse;
import com.edu.unimag.rodatours.services.impl.AvailabilityServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceImplTest {
    @Mock
    private TourScheduleService tourScheduleService;

    @InjectMocks
    private AvailabilityServiceImpl service;

    @Test
    void reusesScheduleAvailabilityWithoutChangingResults() {
        LocalDate date = LocalDate.of(2027, 1, 15);
        List<AvailabilitySlotResponse> slots = List.of();
        when(tourScheduleService.getAvailableSlots(1L, date, 2)).thenReturn(slots);
        assertSame(slots, service.getAvailableSlots(1L, date, 2));
        verify(tourScheduleService).getAvailableSlots(1L, date, 2);
    }

    @Test
    void preservesDomainErrors() {
        LocalDate date = LocalDate.of(2027, 1, 15);
        var error = new com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException("Tour inexistente");
        when(tourScheduleService.getAvailableSlots(1L, date, 1)).thenThrow(error);
        assertSame(error, assertThrows(error.getClass(), () -> service.getAvailableSlots(1L, date, 1)));
    }
}
