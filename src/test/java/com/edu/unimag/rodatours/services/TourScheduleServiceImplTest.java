package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.api.dtos.request.CreateTourScheduleRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTourScheduleRequest;
import com.edu.unimag.rodatours.api.dtos.response.AvailabilitySlotResponse;
import com.edu.unimag.rodatours.api.dtos.response.TourScheduleResponse;
import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.Guide;
import com.edu.unimag.rodatours.domain.entities.Reservation;
import com.edu.unimag.rodatours.domain.entities.Tour;
import com.edu.unimag.rodatours.domain.entities.TourSchedule;
import com.edu.unimag.rodatours.domain.entities.enums.GuideStatus;
import com.edu.unimag.rodatours.domain.entities.enums.TourStatus;
import com.edu.unimag.rodatours.domain.repositories.GuideRepository;
import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import com.edu.unimag.rodatours.domain.repositories.TourScheduleRepository;
import com.edu.unimag.rodatours.services.exceptions.BusinessException;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.impl.TourScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourScheduleServiceImplTest {

    @Mock
    private TourScheduleRepository tourScheduleRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private GuideRepository guideRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private TourScheduleServiceImpl tourScheduleService;

    private Agency agency;
    private Tour activeTour;
    private Guide activeGuide;

    @BeforeEach
    void setUp() {
        agency = Agency.builder().id(1L).nombreComercial("Agencia Sol").build();
        activeTour = Tour.builder()
                .id(10L)
                .agencia(agency)
                .titulo("Tour Tayrona")
                .duracionHoras(new BigDecimal("3.0"))
                .estado(TourStatus.ACTIVE)
                .build();
        activeGuide = Guide.builder()
                .id(5L)
                .agencia(agency)
                .nombreCompleto("Luis Guía")
                .estado(GuideStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("create() programa salida exitosamente asignando guía válido")
    void create_success() {
        LocalDate fecha = LocalDate.now().plusDays(5);
        LocalTime hora = LocalTime.of(9, 0);
        CreateTourScheduleRequest request = new CreateTourScheduleRequest(5L, fecha, hora, 12);

        when(tourRepository.findById(10L)).thenReturn(Optional.of(activeTour));
        when(tourScheduleRepository.existsByTourIdAndFechaTourAndHoraInicio(10L, fecha, hora)).thenReturn(false);
        when(guideRepository.findById(5L)).thenReturn(Optional.of(activeGuide));
        when(tourScheduleRepository.findByGuiaIdAndFechaTourInOrderByHoraInicioAsc(eq(5L), anyList()))
                .thenReturn(List.of());
        when(tourScheduleRepository.save(any(TourSchedule.class))).thenAnswer(inv -> {
            TourSchedule s = inv.getArgument(0);
            s.setId(50L);
            return s;
        });

        TourScheduleResponse response = tourScheduleService.create(10L, request);

        assertEquals(50L, response.id());
        assertEquals(10L, response.tourId());
        assertEquals(5L, response.guiaId());
        assertEquals(12, response.cuposTotales());
        assertEquals(12, response.cuposDisponibles());
    }

    @Test
    @DisplayName("create() lanza BusinessException si el tour está inactivo o la fecha es pasada")
    void create_inactiveTourOrPastDate_throwsBusinessException() {
        activeTour.setEstado(TourStatus.INACTIVE);
        when(tourRepository.findById(10L)).thenReturn(Optional.of(activeTour));

        CreateTourScheduleRequest req1 = new CreateTourScheduleRequest(null, LocalDate.now().plusDays(1), LocalTime.of(10, 0), 10);
        assertThrows(BusinessException.class, () -> tourScheduleService.create(10L, req1));

        activeTour.setEstado(TourStatus.ACTIVE);
        CreateTourScheduleRequest reqPast = new CreateTourScheduleRequest(null, LocalDate.now().minusDays(1), LocalTime.of(10, 0), 10);
        assertThrows(BusinessException.class, () -> tourScheduleService.create(10L, reqPast));
    }

    @Test
    @DisplayName("create() lanza ConflictException si ya existe salida en misma fecha y hora")
    void create_duplicateSlot_throwsConflictException() {
        LocalDate fecha = LocalDate.now().plusDays(2);
        LocalTime hora = LocalTime.of(8, 30);

        when(tourRepository.findById(10L)).thenReturn(Optional.of(activeTour));
        when(tourScheduleRepository.existsByTourIdAndFechaTourAndHoraInicio(10L, fecha, hora)).thenReturn(true);

        CreateTourScheduleRequest request = new CreateTourScheduleRequest(null, fecha, hora, 10);
        assertThrows(ConflictException.class, () -> tourScheduleService.create(10L, request));
    }

    @Test
    @DisplayName("assignGuide() valida agencia del guía, estado activo y traslapes horarios")
    void assignGuide_validationsAndSuccess() {
        LocalDate fecha = LocalDate.now().plusDays(4);
        TourSchedule schedule = TourSchedule.builder()
                .id(50L)
                .tour(activeTour)
                .fechaTour(fecha)
                .horaInicio(LocalTime.of(9, 0))
                .cuposTotales(10)
                .cuposDisponibles(10)
                .build();

        when(tourScheduleRepository.findById(50L)).thenReturn(Optional.of(schedule));

        // Guía de otra agencia
        Agency otherAgency = Agency.builder().id(99L).build();
        Guide otherAgencyGuide = Guide.builder().id(6L).agencia(otherAgency).estado(GuideStatus.ACTIVE).build();
        when(guideRepository.findById(6L)).thenReturn(Optional.of(otherAgencyGuide));
        assertThrows(BusinessException.class, () -> tourScheduleService.assignGuide(50L, 6L));

        // Guía con horario traslapado (10:00 a 13:00 se traslapa con 09:00 a 12:00)
        TourSchedule overlapping = TourSchedule.builder()
                .id(51L)
                .tour(activeTour)
                .fechaTour(fecha)
                .horaInicio(LocalTime.of(10, 0))
                .build();
        when(guideRepository.findById(5L)).thenReturn(Optional.of(activeGuide));
        when(tourScheduleRepository.findByGuiaIdAndFechaTourInOrderByHoraInicioAsc(eq(5L), anyList()))
                .thenReturn(List.of(overlapping));
        assertThrows(ConflictException.class, () -> tourScheduleService.assignGuide(50L, 5L));

        // Asignación válida sin traslape
        when(tourScheduleRepository.findByGuiaIdAndFechaTourInOrderByHoraInicioAsc(eq(5L), anyList()))
                .thenReturn(List.of());
        when(tourScheduleRepository.save(any(TourSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        TourScheduleResponse response = tourScheduleService.assignGuide(50L, 5L);
        assertEquals(5L, response.guiaId());
    }

    @Test
    @DisplayName("update() recalcula cuposDisponibles y rechaza reducir cuposTotales por debajo de ocupados")
    void update_recalculatesSpotsAndValidatesOccupied() {
        LocalDate fecha = LocalDate.now().plusDays(4);
        LocalTime hora = LocalTime.of(9, 0);
        TourSchedule schedule = TourSchedule.builder()
                .id(50L)
                .tour(activeTour)
                .fechaTour(fecha)
                .horaInicio(hora)
                .cuposTotales(10)
                .cuposDisponibles(4) // 6 ocupados
                .build();

        when(tourScheduleRepository.findById(50L)).thenReturn(Optional.of(schedule));

        // Intentar bajar cuposTotales a 5 (< 6 ocupados)
        UpdateTourScheduleRequest invalidReq = new UpdateTourScheduleRequest(null, fecha, hora, 5);
        assertThrows(BusinessException.class, () -> tourScheduleService.update(50L, invalidReq));

        // Subir cuposTotales a 15 -> disponibles deben quedar en 15 - 6 = 9
        when(tourScheduleRepository.save(any(TourSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateTourScheduleRequest validReq = new UpdateTourScheduleRequest(null, fecha, hora, 15);
        TourScheduleResponse response = tourScheduleService.update(50L, validReq);

        assertEquals(15, response.cuposTotales());
        assertEquals(9, response.cuposDisponibles());
    }

    @Test
    @DisplayName("update() rechaza reducir cuposTotales por debajo de ocupados + retenidos por reservas SCHEDULED")
    void update_rejectsReductionBelowPendingHolds() {
        LocalDate fecha = LocalDate.now().plusDays(4);
        LocalTime hora = LocalTime.of(9, 0);
        TourSchedule schedule = TourSchedule.builder()
                .id(50L)
                .tour(activeTour)
                .fechaTour(fecha)
                .horaInicio(hora)
                .cuposTotales(10)
                .cuposDisponibles(6) // 4 confirmados
                .build();

        when(tourScheduleRepository.findById(50L)).thenReturn(Optional.of(schedule));
        when(reservationRepository.sumPersonasByHorarioIdAndEstado(50L, ReservationStatus.SCHEDULED))
                .thenReturn(3L); // 4 + 3 = 7 comprometidos

        UpdateTourScheduleRequest invalidReq = new UpdateTourScheduleRequest(null, fecha, hora, 6);
        assertThrows(BusinessException.class, () -> tourScheduleService.update(50L, invalidReq));
        verify(tourScheduleRepository, never()).save(any(TourSchedule.class));
    }

    @Test
    @DisplayName("delete() lanza ConflictException si la salida tiene reservas asociadas")
    void delete_withReservations_throwsConflictException() {
        TourSchedule schedule = TourSchedule.builder()
                .id(50L)
                .reservas(new ArrayList<>(List.of(Reservation.builder().id(1L).build())))
                .build();

        when(tourScheduleRepository.findById(50L)).thenReturn(Optional.of(schedule));

        assertThrows(ConflictException.class, () -> tourScheduleService.delete(50L));
        verify(tourScheduleRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getAvailableSlots() devuelve lista vacía si el tour está inactivo o franjas disponibles si está activo")
    void getAvailableSlots_success() {
        LocalDate fecha = LocalDate.now().plusDays(2);
        activeTour.setEstado(TourStatus.INACTIVE);
        when(tourRepository.findById(10L)).thenReturn(Optional.of(activeTour));

        assertTrue(tourScheduleService.getAvailableSlots(10L, fecha, 2).isEmpty());

        activeTour.setEstado(TourStatus.ACTIVE);
        TourSchedule slot = TourSchedule.builder()
                .id(50L)
                .tour(activeTour)
                .fechaTour(fecha)
                .horaInicio(LocalTime.of(8, 0))
                .cuposTotales(10)
                .cuposDisponibles(5)
                .build();
        when(tourScheduleRepository.findWithAvailableSpots(10L, fecha, 2)).thenReturn(List.of(slot));

        List<AvailabilitySlotResponse> slots = tourScheduleService.getAvailableSlots(10L, fecha, 2);
        assertEquals(1, slots.size());
        assertEquals(5, slots.get(0).cuposDisponibles());
    }

    @Test
    @DisplayName("update() rechaza reprogramar fecha u hora con tour inactivo, fecha pasada o reservas activas")
    void update_reschedule_validations() {
        LocalDate fecha = LocalDate.now().plusDays(4);
        LocalTime hora = LocalTime.of(9, 0);
        TourSchedule schedule = TourSchedule.builder()
                .id(50L)
                .tour(activeTour)
                .fechaTour(fecha)
                .horaInicio(hora)
                .cuposTotales(10)
                .cuposDisponibles(10)
                .build();
        when(tourScheduleRepository.findById(50L)).thenReturn(Optional.of(schedule));

        // Fecha pasada
        UpdateTourScheduleRequest pastReq = new UpdateTourScheduleRequest(null, LocalDate.now().minusDays(1), hora, 10);
        assertThrows(BusinessException.class, () -> tourScheduleService.update(50L, pastReq));

        // Reservas activas
        when(reservationRepository.existsByHorarioIdAndEstadoIn(eq(50L), anyCollection())).thenReturn(true);
        UpdateTourScheduleRequest moveReq = new UpdateTourScheduleRequest(null, fecha.plusDays(1), hora, 10);
        assertThrows(ConflictException.class, () -> tourScheduleService.update(50L, moveReq));

        // Tour inactivo
        activeTour.setEstado(TourStatus.INACTIVE);
        assertThrows(BusinessException.class, () -> tourScheduleService.update(50L, moveReq));

        verify(tourScheduleRepository, never()).save(any(TourSchedule.class));
    }
}
