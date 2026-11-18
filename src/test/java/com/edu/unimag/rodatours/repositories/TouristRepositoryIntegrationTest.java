package com.edu.unimag.rodatours.repositories;

import com.edu.unimag.rodatours.entities.Agency;
import com.edu.unimag.rodatours.entities.ExperienceType;
import com.edu.unimag.rodatours.entities.Reservation;
import com.edu.unimag.rodatours.entities.Tour;
import com.edu.unimag.rodatours.entities.TourSchedule;
import com.edu.unimag.rodatours.entities.Tourist;
import com.edu.unimag.rodatours.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.entities.enums.TouristStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TouristRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

    @Autowired
    private TouristRepository repository;

    @Test
    @DisplayName("Busca un turista por correo electrónico y valida su unicidad")
    void findByCorreoElectronico() {
        Tourist turista = TestDataFactory.tourist(entityManager, "ana", TouristStatus.ACTIVE);

        Optional<Tourist> encontrado = repository.findByCorreoElectronico(turista.getCorreoElectronico());
        assertTrue(encontrado.isPresent());
        assertEquals(turista.getId(), encontrado.get().getId());
        assertTrue(repository.existsByCorreoElectronico(turista.getCorreoElectronico()));
        assertTrue(repository.findByCorreoElectronico("no.existe@example.com").isEmpty());
        assertTrue(repository.findByCorreoElectronicoIgnoreCase(
                turista.getCorreoElectronico().toUpperCase()).isPresent());

        Tourist duplicado = Tourist.builder()
                .nombreCompleto("Otro turista")
                .correoElectronico(turista.getCorreoElectronico())
                .contrasena("hash")
                .estado(TouristStatus.ACTIVE)
                .build();
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicado));
    }

    @Test
    @DisplayName("Filtra turistas por estado y por nombre ignorando mayúsculas")
    void findByEstadoYNombre() {
        TestDataFactory.tourist(entityManager, "activo", TouristStatus.ACTIVE);
        TestDataFactory.tourist(entityManager, "inactivo", TouristStatus.INACTIVE);

        List<Tourist> activos = repository.findByEstado(TouristStatus.ACTIVE);
        assertEquals(1, activos.size());
        assertEquals("Turista activo", activos.getFirst().getNombreCompleto());
        assertEquals(1, repository.findByEstadoOrderByNombreCompletoAsc(TouristStatus.INACTIVE).size());
        assertEquals(1, repository.findByEstadoAndNombreCompletoContainingIgnoreCase(
                TouristStatus.ACTIVE, "ACTIVO").size());
        assertTrue(repository.findByEstadoAndNombreCompletoContainingIgnoreCase(
                TouristStatus.ACTIVE, "inactivo").isEmpty());
    }

    @Test
    @DisplayName("Encuentra al turista dueño de una reserva")
    void findByReservaId() {
        Agency agency = TestDataFactory.agency(entityManager, "a");
        ExperienceType type = TestDataFactory.experienceType(entityManager, "Aventura");
        Tour tour = TestDataFactory.tour(entityManager, agency, type, "Minca");
        TourSchedule schedule = TestDataFactory.schedule(entityManager, tour, java.time.LocalTime.of(9, 0), 10);
        Tourist turista = TestDataFactory.tourist(entityManager, "reservante");
        Reservation reserva = TestDataFactory.reservation(entityManager, turista, schedule,
                ReservationStatus.SCHEDULED, LocalDateTime.of(2026, 12, 10, 9, 0), 2);

        Optional<Tourist> encontrado = repository.findByReservaId(reserva.getId());
        assertTrue(encontrado.isPresent());
        assertEquals(turista.getId(), encontrado.get().getId());
        assertTrue(repository.findByReservaId(999L).isEmpty());
    }

    @Test
    @DisplayName("PrePersist asigna fecha de creación y estado activo por defecto")
    void prePersistDefaults() {
        Tourist turista = TestDataFactory.tourist(entityManager, "nuevo");

        assertNotNull(turista.getId());
        assertNotNull(turista.getFechaCreacion());
        assertEquals(TouristStatus.ACTIVE, turista.getEstado());
    }
}
