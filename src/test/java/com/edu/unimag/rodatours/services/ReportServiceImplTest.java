package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.services.impl.ReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {
    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReportServiceImpl service;

    @Test
    void delegatesReportsPreservingDateRanges() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);
        var productivity = List.<com.edu.unimag.rodatours.api.dtos.response.AgencyProductivityResponse>of();
        var noShows = List.<com.edu.unimag.rodatours.api.dtos.response.NoShowTouristResponse>of();
        when(reservationService.getAgencyProductivityRanking(desde, hasta)).thenReturn(productivity);
        when(reservationService.getNoShowTouristsReport(desde, hasta)).thenReturn(noShows);
        service.getAgencyOccupancy(1L, desde, hasta);
        assertSame(productivity, service.getAgencyProductivityRanking(desde, hasta));
        assertSame(noShows, service.getNoShowTouristsReport(desde, hasta));
        verify(reservationService).getAgencyOccupancy(1L, desde, hasta);
    }
}
