package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.api.dtos.request.CancelReservationRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateReservationRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateReservationRequest;
import com.edu.unimag.rodatours.api.dtos.response.AgencyOccupancyResponse;
import com.edu.unimag.rodatours.api.dtos.response.AgencyProductivityResponse;
import com.edu.unimag.rodatours.api.dtos.response.NoShowTouristResponse;
import com.edu.unimag.rodatours.api.dtos.response.ReservationResponse;
import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.Reservation;
import com.edu.unimag.rodatours.domain.entities.Tour;
import com.edu.unimag.rodatours.domain.entities.TourSchedule;
import com.edu.unimag.rodatours.domain.entities.Tourist;
import com.edu.unimag.rodatours.domain.entities.enums.AgencyStatus;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.entities.enums.TourStatus;
import com.edu.unimag.rodatours.domain.entities.enums.TouristStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.domain.repositories.TourScheduleRepository;
import com.edu.unimag.rodatours.domain.repositories.TouristRepository;
import com.edu.unimag.rodatours.domain.repositories.projections.AgencyOccupancyProjection;
import com.edu.unimag.rodatours.domain.repositories.projections.AgencyProductivityProjection;
import com.edu.unimag.rodatours.domain.repositories.projections.NoShowTouristProjection;
import com.edu.unimag.rodatours.services.exceptions.BusinessException;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TouristRepository touristRepository;

    @Mock
    private TourScheduleRepository tourScheduleRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher events;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Tourist activeTourist;
    private Agency activeAgency;
    private Tour activeTour;
    private TourSchedule futureSchedule;

    @BeforeEach
    void setUp() {
        activeTourist = Tourist.builder()
                .id(1L)
                .nombreCompleto("Carlos Pérez")
                .correoElectronico("carlos@example.com")
                .estado(TouristStatus.ACTIVE)
                .build();

        activeAgency = Agency.builder()
                .id(10L)
                .nombreComercial("Rodadero Tours")
                .estado(AgencyStatus.ACTIVE)
                .build();

        activeTour = Tour.builder()
                .id(20L)
                .agencia(activeAgency)
                .titulo("Tour Bahía Concha")
                .precio(new BigDecimal("120000.00"))
                .duracionHoras(new BigDecimal("4.0"))
                .estado(TourStatus.ACTIVE)
                .build();

        futureSchedule = TourSchedule.builder()
                .id(30L)
                .tour(activeTour)
                .fechaTour(LocalDate.now().plusDays(3))
                .horaInicio(LocalTime.of(8, 0))
                .cuposTotales(15)
                .cuposDisponibles(10)
                .build();
    }

    @Test
    @DisplayName("create() crea reserva exitosamente calculando precio total, fechas y código QR")
    void create_success() {
        CreateReservationRequest request = new CreateReservationRequest(1L, 30L, 3, "Sin alergias");

        when(touristRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeTourist));
        when(tourScheduleRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(futureSchedule));
        when(reservationRepository.existsOverlappingByTuristaAndStates(eq(1L), anyList(), any(), any()))
                .thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        ReservationResponse response = reservationService.create(request);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(1L, response.turistaId());
        assertEquals(30L, response.horarioId());
        assertEquals(3, response.cantidadPersonas());
        assertEquals(new BigDecimal("360000.00"), response.precioTotal());
        assertEquals(ReservationStatus.SCHEDULED, response.estado());
        assertNotNull(response.codigoQr());
        assertEquals(futureSchedule.getStartDateTime().plusHours(4), response.fechaFin());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("create() lanza BusinessException si el turista está inactivo")
    void create_inactiveTourist_throwsBusinessException() {
        activeTourist.setEstado(TouristStatus.INACTIVE);
        when(touristRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeTourist));

        CreateReservationRequest request = new CreateReservationRequest(1L, 30L, 2, null);

        assertThrows(BusinessException.class, () -> reservationService.create(request));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("create() lanza BusinessException si el tour o la agencia están inactivos")
    void create_inactiveTourOrAgency_throwsBusinessException() {
        activeTour.setEstado(TourStatus.INACTIVE);
        when(touristRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeTourist));
        when(tourScheduleRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(futureSchedule));

        CreateReservationRequest request = new CreateReservationRequest(1L, 30L, 2, null);

        assertThrows(BusinessException.class, () -> reservationService.create(request));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("create() lanza BusinessException si la salida ya ocurrió en el pasado")
    void create_pastSchedule_throwsBusinessException() {
        futureSchedule.setFechaTour(LocalDate.now().minusDays(1));
        when(touristRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeTourist));
        when(tourScheduleRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(futureSchedule));

        CreateReservationRequest request = new CreateReservationRequest(1L, 30L, 2, null);

        assertThrows(BusinessException.class, () -> reservationService.create(request));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("create() lanza ConflictException cuando no hay cupos suficientes")
    void create_insufficientSpots_throwsConflictException() {
        futureSchedule.setCuposDisponibles(1);
        when(touristRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeTourist));
        when(tourScheduleRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(futureSchedule));

        CreateReservationRequest request = new CreateReservationRequest(1L, 30L, 2, null);

        assertThrows(ConflictException.class, () -> reservationService.create(request));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("create() cuenta como ocupados los cupos retenidos por reservas SCHEDULED y lanza ConflictException")
    void create_pendingReservationsHoldSpots_throwsConflictException() {
        // 10 disponibles, 9 retenidos por pendientes -> solo queda 1 cupo real
        when(touristRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeTourist));
        when(tourScheduleRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(futureSchedule));
        when(reservationRepository.sumPersonasByHorarioIdAndEstado(30L, ReservationStatus.SCHEDULED))
                .thenReturn(9L);

        CreateReservationRequest request = new CreateReservationRequest(1L, 30L, 2, null);

        assertThrows(ConflictException.class, () -> reservationService.create(request));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("create() lanza ConflictException si el turista tiene una reserva activa traslapada")
    void create_overlappingReservation_throwsConflictException() {
        when(touristRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeTourist));
        when(tourScheduleRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(futureSchedule));
        when(reservationRepository.existsOverlappingByTuristaAndStates(eq(1L), anyList(), any(), any()))
                .thenReturn(true);

        CreateReservationRequest request = new CreateReservationRequest(1L, 30L, 2, null);

        assertThrows(ConflictException.class, () -> reservationService.create(request));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirm() confirma la reserva y descuenta cupos usando bloqueo pesimista")
    void confirm_success_discountsSpots() {
        Reservation reservation = Reservation.builder()
                .id(100L)
                .turista(activeTourist)
                .horario(futureSchedule)
                .cantidadPersonas(4)
                .estado(ReservationStatus.SCHEDULED)
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(tourScheduleRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(futureSchedule));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = reservationService.confirm(100L);

        assertEquals(ReservationStatus.CONFIRMED, response.estado());
        assertEquals(6, futureSchedule.getCuposDisponibles());
        verify(tourScheduleRepository).save(futureSchedule);
        verify(reservationRepository).save(reservation);
    }

    @Test
    @DisplayName("confirm() lanza ConflictException si el estado no permite transición a CONFIRMED")
    void confirm_invalidState_throwsConflictException() {
        Reservation reservation = Reservation.builder()
                .id(100L)
                .estado(ReservationStatus.CANCELLED)
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThrows(ConflictException.class, () -> reservationService.confirm(100L));
        verify(tourScheduleRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("cancel() libera cupos cuando la reserva estaba en estado CONFIRMED")
    void cancel_confirmedReservation_releasesSpots() {
        futureSchedule.setCuposDisponibles(6);
        Reservation reservation = Reservation.builder()
                .id(100L)
                .turista(activeTourist)
                .horario(futureSchedule)
                .cantidadPersonas(4)
                .estado(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(tourScheduleRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(futureSchedule));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = reservationService.cancel(100L, new CancelReservationRequest("Vuelo retrasado"));

        assertEquals(ReservationStatus.CANCELLED, response.estado());
        assertEquals("Vuelo retrasado", response.motivoCancelacion());
        assertEquals(10, futureSchedule.getCuposDisponibles());
        verify(tourScheduleRepository).save(futureSchedule);
    }

    @Test
    @DisplayName("cancel() no modifica cupos si la reserva estaba en estado SCHEDULED")
    void cancel_scheduledReservation_doesNotReleaseSpots() {
        Reservation reservation = Reservation.builder()
                .id(100L)
                .turista(activeTourist)
                .horario(futureSchedule)
                .cantidadPersonas(4)
                .estado(ReservationStatus.SCHEDULED)
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = reservationService.cancel(100L, new CancelReservationRequest("Cambio de fecha"));

        assertEquals(ReservationStatus.CANCELLED, response.estado());
        assertEquals(10, futureSchedule.getCuposDisponibles());
        verify(tourScheduleRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("cancel() lanza BusinessException si el motivo es nulo o vacío y no libera cupos")
    void cancel_withoutReason_throwsBusinessException() {
        futureSchedule.setCuposDisponibles(6);
        Reservation reservation = Reservation.builder()
                .id(100L)
                .turista(activeTourist)
                .horario(futureSchedule)
                .cantidadPersonas(4)
                .estado(ReservationStatus.CONFIRMED)
                .build();
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThrows(BusinessException.class,
                () -> reservationService.cancel(100L, new CancelReservationRequest("   ")));
        assertThrows(BusinessException.class, () -> reservationService.cancel(100L, null));

        verify(tourScheduleRepository, never()).findByIdForUpdate(any());
        assertEquals(6, futureSchedule.getCuposDisponibles());
        assertEquals(ReservationStatus.CONFIRMED, reservation.getEstado());
    }

    @Test
    @DisplayName("cancel() lanza BusinessException si la salida ya inició")
    void cancel_tourAlreadyStarted_throwsBusinessException() {
        futureSchedule.setFechaTour(LocalDate.now().minusDays(1));
        Reservation reservation = Reservation.builder()
                .id(100L)
                .turista(activeTourist)
                .horario(futureSchedule)
                .cantidadPersonas(2)
                .estado(ReservationStatus.CONFIRMED)
                .build();
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThrows(BusinessException.class,
                () -> reservationService.cancel(100L, new CancelReservationRequest("Tarde")));
        verify(tourScheduleRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("getAgencyOccupancy() cuenta CONFIRMED, COMPLETED y NO_SHOW como cupos ocupados")
    void getAgencyOccupancy_countsNoShowAsOccupied() {
        LocalDate desde = LocalDate.of(2026, 11, 1);
        LocalDate hasta = LocalDate.of(2026, 11, 30);
        AgencyOccupancyProjection occProj = mock(AgencyOccupancyProjection.class);
        when(occProj.getAgenciaId()).thenReturn(10L);
        when(occProj.getNombreComercial()).thenReturn("Rodadero Tours");
        when(occProj.getCuposTotales()).thenReturn(100L);
        when(occProj.getCuposOcupados()).thenReturn(50L);
        when(reservationRepository.findAgencyOccupancy(eq(10L), eq(desde), eq(hasta), anyCollection()))
                .thenReturn(Optional.of(occProj));

        reservationService.getAgencyOccupancy(10L, desde, hasta);

        verify(reservationRepository).findAgencyOccupancy(eq(10L), eq(desde), eq(hasta), argThat(estados ->
                estados.containsAll(List.of(
                        ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED, ReservationStatus.NO_SHOW))));
    }

    @Test
    @DisplayName("complete() marca como COMPLETED una reserva confirmada cuya fecha de tour no es futura")
    void complete_success() {
        futureSchedule.setFechaTour(LocalDate.now());
        Reservation reservation = Reservation.builder()
                .id(100L)
                .turista(activeTourist)
                .horario(futureSchedule)
                .estado(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = reservationService.complete(100L, "Todo sin novedad");

        assertEquals(ReservationStatus.COMPLETED, response.estado());
        assertEquals("Todo sin novedad", response.observaciones());
    }

    @Test
    @DisplayName("complete() lanza BusinessException si la fecha del tour aún es futura")
    void complete_futureTourDate_throwsBusinessException() {
        futureSchedule.setFechaTour(LocalDate.now().plusDays(2));
        Reservation reservation = Reservation.builder()
                .id(100L)
                .horario(futureSchedule)
                .estado(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThrows(BusinessException.class, () -> reservationService.complete(100L, null));
    }

    @Test
    @DisplayName("markNoShow() marca NO_SHOW para una reserva confirmada de hoy o anterior")
    void markNoShow_success() {
        futureSchedule.setFechaTour(LocalDate.now());
        Reservation reservation = Reservation.builder()
                .id(100L)
                .turista(activeTourist)
                .horario(futureSchedule)
                .estado(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = reservationService.markNoShow(100L);

        assertEquals(ReservationStatus.NO_SHOW, response.estado());
    }

    @Test
    @DisplayName("getByQrCode() devuelve la reserva o lanza ResourceNotFoundException")
    void getByQrCode_foundAndNotFound() {
        Reservation reservation = Reservation.builder()
                .id(100L)
                .codigoQr("QR-999")
                .estado(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findByCodigoQr("QR-999")).thenReturn(Optional.of(reservation));
        when(reservationRepository.findByCodigoQr("INEXISTENTE")).thenReturn(Optional.empty());

        assertEquals(100L, reservationService.getByQrCode("QR-999").id());
        assertThrows(ResourceNotFoundException.class, () -> reservationService.getByQrCode("INEXISTENTE"));
    }

    @Test
    @DisplayName("updateNotes() actualiza únicamente las observaciones de la reserva")
    void updateNotes_success() {
        Reservation reservation = Reservation.builder()
                .id(100L)
                .observaciones("Nota vieja")
                .estado(ReservationStatus.SCHEDULED)
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = reservationService.updateNotes(100L, new UpdateReservationRequest("Nota nueva"));

        assertEquals("Nota nueva", response.observaciones());
    }

    @Test
    @DisplayName("Reportes de ocupación, ranking de productividad y no-show funcionan y validan rango de fechas")
    void reports_successAndInvalidDateRange() {
        LocalDate desde = LocalDate.of(2026, 11, 1);
        LocalDate hasta = LocalDate.of(2026, 11, 30);

        assertThrows(BusinessException.class, () -> reservationService.getAgencyOccupancy(10L, hasta, desde));
        assertThrows(BusinessException.class, () -> reservationService.getByReservationDateRange(
                LocalDateTime.now().plusDays(1), LocalDateTime.now()));

        AgencyOccupancyProjection occProj = mock(AgencyOccupancyProjection.class);
        when(occProj.getAgenciaId()).thenReturn(10L);
        when(occProj.getNombreComercial()).thenReturn("Rodadero Tours");
        when(occProj.getCuposTotales()).thenReturn(100L);
        when(occProj.getCuposOcupados()).thenReturn(75L);
        when(reservationRepository.findAgencyOccupancy(eq(10L), eq(desde), eq(hasta), anyList()))
                .thenReturn(Optional.of(occProj));

        AgencyOccupancyResponse occResp = reservationService.getAgencyOccupancy(10L, desde, hasta);
        assertEquals(new BigDecimal("75.00"), occResp.porcentajeOcupacion());

        AgencyProductivityProjection prodProj = mock(AgencyProductivityProjection.class);
        when(prodProj.getAgenciaId()).thenReturn(10L);
        when(prodProj.getNombreComercial()).thenReturn("Rodadero Tours");
        when(prodProj.getToursCompletados()).thenReturn(12L);
        when(reservationRepository.rankingAgenciesByCompletedTours(ReservationStatus.COMPLETED, desde, hasta))
                .thenReturn(List.of(prodProj));

        List<AgencyProductivityResponse> ranking = reservationService.getAgencyProductivityRanking(desde, hasta);
        assertEquals(1, ranking.size());
        assertEquals(1, ranking.get(0).posicion());

        NoShowTouristProjection noShowProj = mock(NoShowTouristProjection.class);
        when(noShowProj.getTuristaId()).thenReturn(1L);
        when(noShowProj.getNombreCompleto()).thenReturn("Carlos Pérez");
        when(noShowProj.getCantidadNoShow()).thenReturn(2L);
        when(reservationRepository.findNoShowTourists(ReservationStatus.NO_SHOW, desde, hasta))
                .thenReturn(List.of(noShowProj));

        List<NoShowTouristResponse> noShows = reservationService.getNoShowTouristsReport(desde, hasta);
        assertEquals(1, noShows.size());
        assertEquals(2L, noShows.get(0).cantidadNoShow());
    }
}
