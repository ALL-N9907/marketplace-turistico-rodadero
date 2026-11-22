package com.edu.unimag.rodatours.repositories;

import com.edu.unimag.rodatours.domain.entities.ExperienceType;
import com.edu.unimag.rodatours.domain.repositories.ExperienceTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExperienceTypeRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

    @Autowired
    private ExperienceTypeRepository repository;

    @Test
    @DisplayName("Busca tipos de experiencia por nombre sin distinguir mayúsculas")
    void findByNombreIgnoreCase() {
        ExperienceType aventura = TestDataFactory.experienceType(entityManager, "Aventura");

        assertTrue(repository.findByNombreIgnoreCase("TIPO AVENTURA").isPresent());
        assertEquals(aventura.getId(), repository.findByNombreIgnoreCase("tipo aventura").orElseThrow().getId());
        assertTrue(repository.existsByNombreIgnoreCase("tipo aventura"));
        assertTrue(repository.findByNombreIgnoreCase("Playa").isEmpty());
    }

    @Test
    @DisplayName("Lista los tipos de experiencia ordenados por nombre")
    void findAllByOrderByNombreAsc() {
        TestDataFactory.experienceType(entityManager, "Naturaleza");
        TestDataFactory.experienceType(entityManager, "Aventura");
        TestDataFactory.experienceType(entityManager, "Cultural");

        List<ExperienceType> tipos = repository.findAllByOrderByNombreAsc();
        assertEquals(3, tipos.size());
        assertEquals("Tipo Aventura", tipos.get(0).getNombre());
        assertEquals("Tipo Cultural", tipos.get(1).getNombre());
        assertEquals("Tipo Naturaleza", tipos.get(2).getNombre());
    }

    @Test
    @DisplayName("Rechaza nombres duplicados de tipo de experiencia")
    void nombreUnico() {
        ExperienceType existente = TestDataFactory.experienceType(entityManager, "Playa");
        ExperienceType duplicado = ExperienceType.builder()
                .nombre(existente.getNombre())
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicado));
    }
}
