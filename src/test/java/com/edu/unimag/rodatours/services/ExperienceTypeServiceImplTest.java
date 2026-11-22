package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import com.edu.unimag.rodatours.api.dtos.request.CreateExperienceTypeRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateExperienceTypeRequest;
import com.edu.unimag.rodatours.api.dtos.response.ExperienceTypeResponse;
import com.edu.unimag.rodatours.domain.entities.ExperienceType;
import com.edu.unimag.rodatours.domain.repositories.ExperienceTypeRepository;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.impl.ExperienceTypeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExperienceTypeServiceImplTest {

    @Mock
    private ExperienceTypeRepository experienceTypeRepository;

    @Mock
    private TourRepository tourRepository;

    @InjectMocks
    private ExperienceTypeServiceImpl experienceTypeService;

    @Test
    @DisplayName("create() crea tipo de experiencia o lanza ConflictException si el nombre ya existe")
    void create_successAndConflict() {
        CreateExperienceTypeRequest request = new CreateExperienceTypeRequest("Buceo", "Inmersiones guiadas");

        when(experienceTypeRepository.existsByNombreIgnoreCase("Buceo")).thenReturn(false);
        when(experienceTypeRepository.save(any(ExperienceType.class))).thenAnswer(inv -> {
            ExperienceType e = inv.getArgument(0);
            e.setId(4L);
            return e;
        });

        ExperienceTypeResponse created = experienceTypeService.create(request);
        assertEquals(4L, created.id());
        assertEquals("Buceo", created.nombre());

        when(experienceTypeRepository.existsByNombreIgnoreCase("Buceo")).thenReturn(true);
        assertThrows(ConflictException.class, () -> experienceTypeService.create(request));
    }

    @Test
    @DisplayName("update() y delete() funcionan o lanzan ResourceNotFoundException / ConflictException")
    void updateAndDelete_behavior() {
        ExperienceType existing = ExperienceType.builder().id(4L).nombre("Buceo").descripcion("Desc").build();
        when(experienceTypeRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(experienceTypeRepository.existsByNombreIgnoreCase("Snorkel")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> experienceTypeService.update(4L, new UpdateExperienceTypeRequest("Snorkel", "Desc")));

        when(experienceTypeRepository.existsByNombreIgnoreCase("Snorkel")).thenReturn(false);
        when(experienceTypeRepository.save(any(ExperienceType.class))).thenAnswer(inv -> inv.getArgument(0));

        ExperienceTypeResponse updated = experienceTypeService.update(4L, new UpdateExperienceTypeRequest("Snorkel", "Nueva"));
        assertEquals("Snorkel", updated.nombre());

        experienceTypeService.delete(4L);
        verify(experienceTypeRepository).delete(existing);

        when(experienceTypeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> experienceTypeService.getById(99L));
    }

    @Test
    @DisplayName("delete() lanza ConflictException si el tipo de experiencia tiene tours asociados")
    void delete_withTours_throwsConflictException() {
        ExperienceType existing = ExperienceType.builder().id(4L).nombre("Buceo").build();
        when(experienceTypeRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(tourRepository.existsByTipoExperienciaId(4L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> experienceTypeService.delete(4L));
        verify(experienceTypeRepository, never()).delete(any(ExperienceType.class));
    }
}
