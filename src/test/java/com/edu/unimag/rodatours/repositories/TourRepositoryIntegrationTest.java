package com.edu.unimag.rodatours.repositories;

import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.ExperienceType;
import com.edu.unimag.rodatours.domain.entities.Tour;
import com.edu.unimag.rodatours.domain.entities.TourSchedule;
import com.edu.unimag.rodatours.domain.entities.enums.DifficultyLevel;
import com.edu.unimag.rodatours.domain.entities.enums.TourStatus;
import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TourRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

    @Autowired
    private TourRepository repository;

    @Test
    @DisplayName("Busca tours de una agencia por dificultad y por destino")
    void findByAgenciaIdAndFilters() {
        Agency agency = TestDataFactory.agency(entityManager, "tours");
        Agency otraAgencia = TestDataFactory.agency(entityManager, "otra");
        ExperienceType type = TestDataFactory.experienceType(entityManager, "Aventura");

        Tour fácil = TestDataFactory.tour(entityManager, agency, type, "Minca");
        Tour difícil = TestDataFactory.tour(entityManager, agency, type, "Tayrona");
        difícil.setNivelDificultad(DifficultyLevel.ALTO);
        entityManager.persistAndFlush(difícil);
        Tour otro = TestDataFactory.tour(entityManager, otraAgencia, type, "Minca2");

        assertEquals(2, repository.findByAgenciaId(agency.getId()).size());
        assertEquals(1, repository.findByAgenciaIdAndNivelDificultad(
                agency.getId(), DifficultyLevel.BAJO).size());
        assertEquals(fácil.getId(), repository.findByAgenciaIdAndNivelDificultad(
                agency.getId(), DifficultyLevel.BAJO).getFirst().getId());
        assertEquals(1, repository.findByAgenciaIdAndDestinoContainingIgnoreCase(
                agency.getId(), "minca").size());
        assertEquals(otro.getId(), repository.findByAgenciaIdAndDestinoContainingIgnoreCase(
                otraAgencia.getId(), "MINCA").getFirst().getId());
    }

    @Test
    @DisplayName("Filtra tours activos por destino")
    void findByEstadoAndDestino() {
        Agency agency = TestDataFactory.agency(entityManager, "destinos");
        ExperienceType type = TestDataFactory.experienceType(entityManager, "Playa");
        TestDataFactory.tour(entityManager, agency, type, "Cristalina", TourStatus.ACTIVE, "Tayrona");
        TestDataFactory.tour(entityManager, agency, type, "Cerrado", TourStatus.INACTIVE, "Tayrona");

        List<Tour> tours = repository.findByEstadoAndDestinoContainingIgnoreCase(TourStatus.ACTIVE, "tayr");

        assertEquals(1, tours.size());
        assertEquals(TourStatus.ACTIVE, tours.getFirst().getEstado());
    }

    @Test
    @DisplayName("Devuelve solo tours activos con salidas con cupo real en la fecha y sin repetir el tour")
    void findAvailableByDestinationAndDate() {
        Agency agency = TestDataFactory.agency(entityManager, "disp");
        ExperienceType type = TestDataFactory.experienceType(entityManager, "Naturaleza");

        Tour conCupo = TestDataFactory.tour(entityManager, agency, type, "ConCupo",
                TourStatus.ACTIVE, "Tayrona");
        TourSchedule s1 = TestDataFactory.schedule(entityManager, conCupo, LocalTime.of(9, 0), 5);
        TestDataFactory.schedule(entityManager, conCupo, LocalTime.of(15, 0), 3);

        Tour sinCupo = TestDataFactory.tour(entityManager, agency, type, "SinCupo",
                TourStatus.ACTIVE, "Tayrona");
        TestDataFactory.schedule(entityManager, sinCupo, null, TestDataFactory.FECHA_TOUR,
                LocalTime.of(10, 0), 1, 0);

        Tour inactivo = TestDataFactory.tour(entityManager, agency, type, "Inactivo",
                TourStatus.INACTIVE, "Tayrona");
        TestDataFactory.schedule(entityManager, inactivo, LocalTime.of(11, 0), 8);

        Tour otroDestino = TestDataFactory.tour(entityManager, agency, type, "OtroDestino",
                TourStatus.ACTIVE, "Minca");
        TestDataFactory.schedule(entityManager, otroDestino, LocalTime.of(12, 0), 8);

        List<Tour> disponibles = repository.findAvailableByDestinationAndDate(
                TourStatus.ACTIVE, "tayr", s1.getFechaTour(), 1);
        assertEquals(1, disponibles.size());
        assertEquals(conCupo.getId(), disponibles.getFirst().getId());

        assertTrue(repository.findAvailableByDestinationAndDate(
                TourStatus.ACTIVE, "Tayrona", s1.getFechaTour(), 6).isEmpty());
        assertTrue(repository.findAvailableByDestinationAndDate(
                TourStatus.ACTIVE, null, s1.getFechaTour(), 1).size() >= 2);
        assertTrue(repository.findAvailableByDestinationAndDate(
                TourStatus.ACTIVE, "Tayrona", s1.getFechaTour().plusDays(1), 1).isEmpty());
    }
}
