package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.api.dtos.request.CreateTouristRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTouristRequest;
import com.edu.unimag.rodatours.api.dtos.response.TouristResponse;
import com.edu.unimag.rodatours.domain.entities.Tourist;
import com.edu.unimag.rodatours.domain.entities.enums.TouristStatus;
import com.edu.unimag.rodatours.domain.repositories.TouristRepository;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.impl.TouristServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TouristServiceImplTest {

    @Mock
    private TouristRepository touristRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ReservationRepository reservationRepository;

    private TouristServiceImpl touristService;

    @BeforeEach
    void setUp() {
        touristService = new TouristServiceImpl(touristRepository, passwordEncoder, reservationRepository);
    }

    @Test
    @DisplayName("create() codifica la contraseña cuando PasswordEncoder está presente y guarda el turista")
    void create_encodesPasswordAndSaves() {
        CreateTouristRequest request = new CreateTouristRequest(
                "Laura Gómez", "laura@example.com", "Clave1234!", "3105556677", null);

        when(touristRepository.existsByCorreoElectronicoIgnoreCase("laura@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Clave1234!")).thenReturn("$2a$10$encodedHash");
        when(touristRepository.save(any(Tourist.class))).thenAnswer(inv -> {
            Tourist t = inv.getArgument(0);
            t.setId(3L);
            return t;
        });

        TouristResponse response = touristService.create(request);

        ArgumentCaptor<Tourist> captor = ArgumentCaptor.forClass(Tourist.class);
        verify(touristRepository).save(captor.capture());
        assertEquals("$2a$10$encodedHash", captor.getValue().getContrasena());
        assertEquals(3L, response.id());
        assertEquals(TouristStatus.ACTIVE, response.estado());
    }

    @Test
    @DisplayName("create() lanza ConflictException si el correo electrónico ya está registrado")
    void create_duplicateEmail_throwsConflictException() {
        CreateTouristRequest request = new CreateTouristRequest(
                "Laura Gómez", "laura@example.com", "Clave1234!", null, null);

        when(touristRepository.existsByCorreoElectronicoIgnoreCase("laura@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> touristService.create(request));
        verify(touristRepository, never()).save(any());
    }

    @Test
    @DisplayName("getByEmail() encuentra al turista sin importar mayúsculas o lanza ResourceNotFoundException")
    void getByEmail_foundAndNotFound() {
        Tourist tourist = Tourist.builder().id(3L).correoElectronico("laura@example.com").build();
        when(touristRepository.findByCorreoElectronicoIgnoreCase("LAURA@EXAMPLE.COM")).thenReturn(Optional.of(tourist));
        when(touristRepository.findByCorreoElectronicoIgnoreCase("no@example.com")).thenReturn(Optional.empty());

        assertEquals(3L, touristService.getByEmail("LAURA@EXAMPLE.COM").id());
        assertThrows(ResourceNotFoundException.class, () -> touristService.getByEmail("no@example.com"));
    }

    @Test
    @DisplayName("delete() lanza ConflictException si el turista tiene reservas y elimina si no las tiene")
    void delete_withReservations_throwsConflictException() {
        Tourist tourist = Tourist.builder().id(3L).correoElectronico("laura@example.com").build();
        when(touristRepository.findById(3L)).thenReturn(Optional.of(tourist));

        when(reservationRepository.existsByTuristaId(3L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> touristService.delete(3L));
        verify(touristRepository, never()).delete(any(Tourist.class));

        when(reservationRepository.existsByTuristaId(3L)).thenReturn(false);
        touristService.delete(3L);
        verify(touristRepository).delete(tourist);
    }

    @Test
    @DisplayName("update() valida correo duplicado al cambiarlo y actualiza datos")
    void update_successAndDuplicateEmail() {
        Tourist existing = Tourist.builder()
                .id(3L)
                .nombreCompleto("Laura")
                .correoElectronico("laura@example.com")
                .estado(TouristStatus.ACTIVE)
                .build();

        when(touristRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(touristRepository.existsByCorreoElectronicoIgnoreCase("otro@example.com")).thenReturn(true);

        UpdateTouristRequest conflictReq = new UpdateTouristRequest("Laura", "otro@example.com", "300", TouristStatus.ACTIVE);
        assertThrows(ConflictException.class, () -> touristService.update(3L, conflictReq));

        when(touristRepository.existsByCorreoElectronicoIgnoreCase("otro@example.com")).thenReturn(false);
        when(touristRepository.save(any(Tourist.class))).thenAnswer(inv -> inv.getArgument(0));

        TouristResponse updated = touristService.update(3L, conflictReq);
        assertEquals("otro@example.com", updated.correoElectronico());
    }

    @Test
    @DisplayName("create() normaliza el correo (trim y minúsculas) y valida unicidad sin distinguir mayúsculas")
    void create_normalizesEmail() {
        CreateTouristRequest request = new CreateTouristRequest(
                "Laura Gómez", "  Laura@Example.COM ", "Clave1234!", null, null);

        when(touristRepository.existsByCorreoElectronicoIgnoreCase("laura@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Clave1234!")).thenReturn("$2a$10$encodedHash");
        when(touristRepository.save(any(Tourist.class))).thenAnswer(inv -> inv.getArgument(0));

        TouristResponse response = touristService.create(request);

        assertEquals("laura@example.com", response.correoElectronico());
        verify(touristRepository).existsByCorreoElectronicoIgnoreCase("laura@example.com");
    }
}
