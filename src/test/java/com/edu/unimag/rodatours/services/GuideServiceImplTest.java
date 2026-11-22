package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.api.dtos.request.CreateGuideRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateGuideRequest;
import com.edu.unimag.rodatours.api.dtos.response.GuideResponse;
import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.Guide;
import com.edu.unimag.rodatours.domain.entities.enums.GuideStatus;
import com.edu.unimag.rodatours.domain.repositories.AgencyRepository;
import com.edu.unimag.rodatours.domain.repositories.GuideRepository;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.impl.GuideServiceImpl;
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
class GuideServiceImplTest {

    @Mock
    private GuideRepository guideRepository;

    @Mock
    private AgencyRepository agencyRepository;

    @InjectMocks
    private GuideServiceImpl guideService;

    @Test
    @DisplayName("create() registra guía válido y detecta duplicados en documento, registro o correo")
    void create_successAndUniqueValidations() {
        Agency agency = Agency.builder().id(1L).build();
        CreateGuideRequest request = new CreateGuideRequest(
                1L, "María López", "1082999888", "RNT-5544", "maria@tours.com", "3004445566", "Español, Inglés", null);

        when(agencyRepository.findById(1L)).thenReturn(Optional.of(agency));
        when(guideRepository.existsByDocumentoIdentidad("1082999888")).thenReturn(false);
        when(guideRepository.existsByRegistroGuia("RNT-5544")).thenReturn(false);
        when(guideRepository.existsByCorreoElectronicoIgnoreCase("maria@tours.com")).thenReturn(false);
        when(guideRepository.save(any(Guide.class))).thenAnswer(inv -> {
            Guide g = inv.getArgument(0);
            g.setId(7L);
            return g;
        });

        GuideResponse response = guideService.create(request);
        assertEquals(7L, response.id());
        assertEquals(GuideStatus.ACTIVE, response.estado());

        when(guideRepository.existsByRegistroGuia("RNT-5544")).thenReturn(true);
        assertThrows(ConflictException.class, () -> guideService.create(request));
    }

    @Test
    @DisplayName("getByAgencyAndStatus() usa ACTIVE por defecto si el estado es null")
    void getByAgencyAndStatus_defaultActive() {
        Agency agency = Agency.builder().id(1L).build();
        Guide guide = Guide.builder().id(7L).agencia(agency).nombreCompleto("María").estado(GuideStatus.ACTIVE).build();

        when(agencyRepository.findById(1L)).thenReturn(Optional.of(agency));
        when(guideRepository.findByAgenciaIdAndEstado(1L, GuideStatus.ACTIVE)).thenReturn(List.of(guide));

        List<GuideResponse> result = guideService.getByAgencyAndStatus(1L, null);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("update() permite conservar los propios campos únicos sin lanzar ConflictException")
    void update_sameUniqueFields_success() {
        Agency agency = Agency.builder().id(1L).build();
        Guide existing = Guide.builder()
                .id(7L)
                .agencia(agency)
                .nombreCompleto("María López")
                .documentoIdentidad("1082999888")
                .registroGuia("RNT-5544")
                .correoElectronico("maria@tours.com")
                .estado(GuideStatus.ACTIVE)
                .build();

        when(guideRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(agencyRepository.findById(1L)).thenReturn(Optional.of(agency));
        when(guideRepository.save(any(Guide.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateGuideRequest request = new UpdateGuideRequest(
                1L, "María C. López", "1082999888", "RNT-5544", "maria@tours.com", "3009998877", "Español", GuideStatus.INACTIVE);

        GuideResponse updated = guideService.update(7L, request);
        assertEquals("María C. López", updated.nombreCompleto());
        assertEquals(GuideStatus.INACTIVE, updated.estado());
    }
}
