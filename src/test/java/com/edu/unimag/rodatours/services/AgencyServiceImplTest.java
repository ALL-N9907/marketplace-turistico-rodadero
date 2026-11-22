package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.domain.repositories.GuideRepository;
import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import com.edu.unimag.rodatours.api.dtos.request.CreateAgencyRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateAgencyRequest;
import com.edu.unimag.rodatours.api.dtos.response.AgencyResponse;
import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.enums.AgencyStatus;
import com.edu.unimag.rodatours.domain.repositories.AgencyRepository;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.impl.AgencyServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgencyServiceImplTest {

    @Mock
    private AgencyRepository agencyRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private GuideRepository guideRepository;

    @InjectMocks
    private AgencyServiceImpl agencyService;

    @Test
    @DisplayName("create() registra agencia nueva o lanza ConflictException si el NIT ya existe")
    void create_successAndConflict() {
        CreateAgencyRequest request = new CreateAgencyRequest(
                "Rodadero Tours", "900123456-1", "Tours marítimos", "Cra 1 # 5-10", "3001112233", null);

        when(agencyRepository.existsByNitORegistro("900123456-1")).thenReturn(false);
        when(agencyRepository.save(any(Agency.class))).thenAnswer(inv -> {
            Agency a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        AgencyResponse created = agencyService.create(request);
        assertEquals(1L, created.id());
        assertEquals(AgencyStatus.ACTIVE, created.estado());

        when(agencyRepository.existsByNitORegistro("900123456-1")).thenReturn(true);
        assertThrows(ConflictException.class, () -> agencyService.create(request));
    }

    @Test
    @DisplayName("searchActiveByNameOrLocation() retorna todas las activas si query es blanco o filtra si tiene texto")
    void searchActiveByNameOrLocation_behavior() {
        Agency agency = Agency.builder().id(1L).nombreComercial("Tayrona Express").estado(AgencyStatus.ACTIVE).build();
        when(agencyRepository.findByEstadoOrderByNombreComercialAsc(AgencyStatus.ACTIVE)).thenReturn(List.of(agency));
        when(agencyRepository.searchActiveByNameOrLocation(AgencyStatus.ACTIVE, "Rodadero")).thenReturn(List.of(agency));

        assertEquals(1, agencyService.searchActiveByNameOrLocation("   ").size());
        assertEquals(1, agencyService.searchActiveByNameOrLocation(" Rodadero ").size());
    }

    @Test
    @DisplayName("update() valida unicidad de NIT cuando cambia y actualiza datos")
    void update_successAndDuplicateNit() {
        Agency existing = Agency.builder()
                .id(1L)
                .nombreComercial("Agencia A")
                .nitORegistro("NIT-1")
                .estado(AgencyStatus.ACTIVE)
                .build();

        when(agencyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(agencyRepository.existsByNitORegistro("NIT-2")).thenReturn(true);

        UpdateAgencyRequest conflictReq = new UpdateAgencyRequest(
                "Agencia A", "NIT-2", null, "Dir", "300", AgencyStatus.ACTIVE);
        assertThrows(ConflictException.class, () -> agencyService.update(1L, conflictReq));

        when(agencyRepository.existsByNitORegistro("NIT-2")).thenReturn(false);
        when(agencyRepository.save(any(Agency.class))).thenAnswer(inv -> inv.getArgument(0));

        AgencyResponse updated = agencyService.update(1L, conflictReq);
        assertEquals("NIT-2", updated.nitORegistro());
    }

    @Test
    @DisplayName("delete() lanza ConflictException si la agencia tiene tours o guías y elimina si no tiene")
    void delete_withDependents_throwsConflictException() {
        Agency agency = Agency.builder().id(1L).nitORegistro("900123456-1").build();
        when(agencyRepository.findById(1L)).thenReturn(Optional.of(agency));

        when(tourRepository.existsByAgenciaId(1L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> agencyService.delete(1L));

        when(tourRepository.existsByAgenciaId(1L)).thenReturn(false);
        when(guideRepository.existsByAgenciaId(1L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> agencyService.delete(1L));
        verify(agencyRepository, never()).delete(any(Agency.class));

        when(guideRepository.existsByAgenciaId(1L)).thenReturn(false);
        agencyService.delete(1L);
        verify(agencyRepository).delete(agency);
    }

    @Test
    @DisplayName("getById() lanza ResourceNotFoundException si no existe")
    void getById_notFound() {
        when(agencyRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> agencyService.getById(99L));
    }
}
