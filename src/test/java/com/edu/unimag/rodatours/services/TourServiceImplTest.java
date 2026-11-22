package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.domain.repositories.TourScheduleRepository;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.api.dtos.request.CreateTourRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTourRequest;
import com.edu.unimag.rodatours.api.dtos.response.TourResponse;
import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.ExperienceType;
import com.edu.unimag.rodatours.domain.entities.Tour;
import com.edu.unimag.rodatours.domain.entities.enums.AgencyStatus;
import com.edu.unimag.rodatours.domain.entities.enums.DifficultyLevel;
import com.edu.unimag.rodatours.domain.entities.enums.TourStatus;
import com.edu.unimag.rodatours.domain.repositories.AgencyRepository;
import com.edu.unimag.rodatours.domain.repositories.ExperienceTypeRepository;
import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import com.edu.unimag.rodatours.services.exceptions.BusinessException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.impl.TourServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourServiceImplTest {

    @Mock
    private TourRepository tourRepository;

    @Mock
    private AgencyRepository agencyRepository;

    @Mock
    private ExperienceTypeRepository experienceTypeRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TourScheduleRepository tourScheduleRepository;

    @InjectMocks
    private TourServiceImpl tourService;

    private Agency activeAgency;
    private ExperienceType experienceType;

    @BeforeEach
    void setUp() {
        activeAgency = Agency.builder().id(1L).nombreComercial("Caribe Tours").estado(AgencyStatus.ACTIVE).build();
        experienceType = ExperienceType.builder().id(2L).nombre("Ecoturismo").build();
    }

    @Test
    @DisplayName("create() crea un tour cuando la agencia está activa y el tipo de experiencia existe")
    void create_success() {
        CreateTourRequest request = new CreateTourRequest(
                1L, 2L, "Caminata Minca", "Cascadas y café", "Minca",
                new BigDecimal("95000.00"), new BigDecimal("5.0"), DifficultyLevel.MEDIO, "Almuerzo", null);

        when(agencyRepository.findById(1L)).thenReturn(Optional.of(activeAgency));
        when(experienceTypeRepository.findById(2L)).thenReturn(Optional.of(experienceType));
        when(tourRepository.save(any(Tour.class))).thenAnswer(inv -> {
            Tour t = inv.getArgument(0);
            t.setId(15L);
            return t;
        });

        TourResponse response = tourService.create(request);

        assertEquals(15L, response.id());
        assertEquals("Caminata Minca", response.titulo());
        assertEquals(TourStatus.ACTIVE, response.estado());
    }

    @Test
    @DisplayName("create() lanza BusinessException si la agencia está inactiva")
    void create_inactiveAgency_throwsBusinessException() {
        activeAgency.setEstado(AgencyStatus.INACTIVE);
        when(agencyRepository.findById(1L)).thenReturn(Optional.of(activeAgency));

        CreateTourRequest request = new CreateTourRequest(
                1L, 2L, "Tour", "Desc", "Minca",
                new BigDecimal("50000.00"), new BigDecimal("2.0"), DifficultyLevel.BAJO, null, null);

        assertThrows(BusinessException.class, () -> tourService.create(request));
        verify(tourRepository, never()).save(any());
    }

    @Test
    @DisplayName("getByAgency() filtra por dificultad, destino o devuelve todos los de la agencia")
    void getByAgency_filtersProperly() {
        Tour tour = Tour.builder().id(15L).agencia(activeAgency).tipoExperiencia(experienceType).titulo("Tour 1").build();
        when(agencyRepository.findById(1L)).thenReturn(Optional.of(activeAgency));
        when(tourRepository.findByAgenciaIdAndNivelDificultad(1L, DifficultyLevel.ALTO)).thenReturn(List.of(tour));
        when(tourRepository.findByAgenciaIdAndDestinoContainingIgnoreCase(1L, "Tayrona")).thenReturn(List.of(tour));
        when(tourRepository.findByAgenciaId(1L)).thenReturn(List.of(tour));
        when(tourRepository.findByAgenciaIdAndNivelDificultadAndDestinoContainingIgnoreCase(
                1L, DifficultyLevel.ALTO, "Tayrona")).thenReturn(List.of(tour));

        assertEquals(1, tourService.getByAgency(1L, DifficultyLevel.ALTO, " Tayrona ").size());
        assertEquals(1, tourService.getByAgency(1L, DifficultyLevel.ALTO, null).size());
        assertEquals(1, tourService.getByAgency(1L, null, " Tayrona ").size());
        assertEquals(1, tourService.getByAgency(1L, null, null).size());
    }

    @Test
    @DisplayName("findAvailableByDestinationAndDate() valida fecha y personas y normaliza destino")
    void findAvailableByDestinationAndDate_validationsAndQuery() {
        LocalDate fecha = LocalDate.now().plusDays(3);

        assertThrows(BusinessException.class, () -> tourService.findAvailableByDestinationAndDate("Minca", null, 2));
        assertThrows(BusinessException.class, () -> tourService.findAvailableByDestinationAndDate("Minca", fecha, 0));

        Tour tour = Tour.builder().id(15L).agencia(activeAgency).tipoExperiencia(experienceType).titulo("Tour").build();
        when(tourRepository.findAvailableByDestinationAndDate(TourStatus.ACTIVE, null, fecha, 2))
                .thenReturn(List.of(tour));

        List<TourResponse> result = tourService.findAvailableByDestinationAndDate("   ", fecha, 2);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("update() y delete() funcionan o lanzan ResourceNotFoundException si no existe el tour")
    void updateAndDelete_successAndNotFound() {
        when(tourRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> tourService.delete(99L));

        Tour existing = Tour.builder().id(15L).agencia(activeAgency).tipoExperiencia(experienceType).build();
        when(tourRepository.findById(15L)).thenReturn(Optional.of(existing));
        when(agencyRepository.findById(1L)).thenReturn(Optional.of(activeAgency));
        when(experienceTypeRepository.findById(2L)).thenReturn(Optional.of(experienceType));
        when(tourRepository.save(any(Tour.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTourRequest updateReq = new UpdateTourRequest(
                1L, 2L, "Nuevo Título", "Nueva Desc", "Rodadero",
                new BigDecimal("80000.00"), new BigDecimal("3.0"), DifficultyLevel.BAJO, "Agua", TourStatus.INACTIVE);

        TourResponse updated = tourService.update(15L, updateReq);
        assertEquals("Nuevo Título", updated.titulo());
        assertEquals(TourStatus.INACTIVE, updated.estado());

        tourService.delete(15L);
        verify(tourRepository).delete(existing);
    }

    @Test
    @DisplayName("delete() lanza ConflictException si el tour tiene reservas asociadas")
    void delete_withReservations_throwsConflictException() {
        Tour existing = Tour.builder().id(15L).agencia(activeAgency).tipoExperiencia(experienceType).build();
        when(tourRepository.findById(15L)).thenReturn(Optional.of(existing));
        when(reservationRepository.existsByHorarioTourId(15L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> tourService.delete(15L));
        verify(tourRepository, never()).delete(any(Tour.class));
    }

    @Test
    @DisplayName("update() rechaza cambiar a una agencia inactiva o cambiar de agencia con reservas o guías asignados")
    void update_changeAgency_validations() {
        Agency otherAgency = Agency.builder().id(7L).nombreComercial("Otra").estado(AgencyStatus.ACTIVE).build();
        Tour existing = Tour.builder().id(15L).agencia(activeAgency).tipoExperiencia(experienceType)
                .duracionHoras(new BigDecimal("3.0")).build();
        when(tourRepository.findById(15L)).thenReturn(Optional.of(existing));
        when(experienceTypeRepository.findById(2L)).thenReturn(Optional.of(experienceType));
        when(agencyRepository.findById(7L)).thenReturn(Optional.of(otherAgency));

        UpdateTourRequest req = new UpdateTourRequest(
                7L, 2L, "T", "D", "Rodadero", new BigDecimal("80000.00"), new BigDecimal("3.0"),
                DifficultyLevel.BAJO, null, null);

        otherAgency.setEstado(AgencyStatus.INACTIVE);
        assertThrows(BusinessException.class, () -> tourService.update(15L, req));

        otherAgency.setEstado(AgencyStatus.ACTIVE);
        when(reservationRepository.existsByHorarioTourId(15L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> tourService.update(15L, req));

        when(reservationRepository.existsByHorarioTourId(15L)).thenReturn(false);
        when(tourScheduleRepository.existsByTourIdAndGuiaIsNotNull(15L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> tourService.update(15L, req));
        verify(tourRepository, never()).save(any(Tour.class));
    }

    @Test
    @DisplayName("update() rechaza cambiar la duración si hay reservas activas")
    void update_changeDuration_withActiveReservations_throwsConflictException() {
        Tour existing = Tour.builder().id(15L).agencia(activeAgency).tipoExperiencia(experienceType)
                .duracionHoras(new BigDecimal("3.0")).build();
        when(tourRepository.findById(15L)).thenReturn(Optional.of(existing));
        when(agencyRepository.findById(1L)).thenReturn(Optional.of(activeAgency));
        when(experienceTypeRepository.findById(2L)).thenReturn(Optional.of(experienceType));
        when(reservationRepository.existsByHorarioTourIdAndEstadoIn(eq(15L), anyCollection())).thenReturn(true);

        UpdateTourRequest req = new UpdateTourRequest(
                1L, 2L, "T", "D", "Rodadero", new BigDecimal("80000.00"), new BigDecimal("5.0"),
                DifficultyLevel.BAJO, null, null);

        assertThrows(ConflictException.class, () -> tourService.update(15L, req));
        verify(tourRepository, never()).save(any(Tour.class));
    }
}
