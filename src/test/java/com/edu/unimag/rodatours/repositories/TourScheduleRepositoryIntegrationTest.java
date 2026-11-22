package com.edu.unimag.rodatours.repositories;

import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.ExperienceType;
import com.edu.unimag.rodatours.domain.entities.Guide;
import com.edu.unimag.rodatours.domain.entities.Tour;
import com.edu.unimag.rodatours.domain.entities.TourSchedule;
import com.edu.unimag.rodatours.domain.repositories.TourScheduleRepository;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TourScheduleRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

    @Autowired
    private TourScheduleRepository repository;

    @Test
    @DisplayName("Lista las salidas de un tour ordenadas por hora")
    void findByTourIdAndFechaTourOrderByHoraInicioAsc() {
        Tour tour = tour("orden");

        TourSchedule tarde = TestDataFactory.schedule(entityManager, tour, LocalTime.of(15, 0), 10);
        TourSchedule temprano = TestDataFactory.schedule(entityManager, tour, LocalTime.of(9, 0), 10);
        TestDataFactory.schedule(entityManager, tour, LocalTime.of(9, 0),
                TestDataFactory.FECHA_TOUR.plusDays(1), 10);

        List<TourSchedule> salidas = repository.findByTourIdAndFechaTourOrderByHoraInicioAsc(
                tour.getId(), TestDataFactory.FECHA_TOUR);

        assertEquals(2, salidas.size());
        assertEquals(temprano.getId(), salidas.getFirst().getId());
        assertEquals(tarde.getId(), salidas.getLast().getId());
        assertEquals(3, repository.findByTourIdOrderByFechaTourAscHoraInicioAsc(tour.getId()).size());
    }

    @Test
    @DisplayName("Filtra salidas con cupos suficientes para la cantidad de personas")
    void findWithAvailableSpots() {
        Tour tour = tour("cupos");
        TourSchedule conCupo = TestDataFactory.schedule(entityManager, tour, LocalTime.of(9, 0), 5);
        TestDataFactory.schedule(entityManager, tour, null, TestDataFactory.FECHA_TOUR,
                LocalTime.of(11, 0), 1, 0);
        TourSchedule pequeno = TestDataFactory.schedule(entityManager, tour, LocalTime.of(13, 0), 2);

        List<TourSchedule> salidas = repository.findWithAvailableSpots(
                tour.getId(), TestDataFactory.FECHA_TOUR, 3);

        assertEquals(1, salidas.size());
        assertEquals(conCupo.getId(), salidas.getFirst().getId());

        List<TourSchedule> paraDos = repository.findWithAvailableSpots(
                tour.getId(), TestDataFactory.FECHA_TOUR, 2);
        assertEquals(2, paraDos.size());
        assertTrue(paraDos.stream().anyMatch(s -> s.getId().equals(pequeno.getId())));
    }

    @Test
    @DisplayName("Detecta salidas duplicadas por tour, fecha y hora")
    void existsByTourIdAndFechaTourAndHoraInicio() {
        Tour tour = tour("unicidad");
        TestDataFactory.schedule(entityManager, tour, LocalTime.of(9, 0), 10);

        assertTrue(repository.existsByTourIdAndFechaTourAndHoraInicio(
                tour.getId(), TestDataFactory.FECHA_TOUR, LocalTime.of(9, 0)));
        assertFalse(repository.existsByTourIdAndFechaTourAndHoraInicio(
                tour.getId(), TestDataFactory.FECHA_TOUR, LocalTime.of(10, 0)));
        assertFalse(repository.existsByTourIdAndFechaTourAndHoraInicio(
                tour.getId(), TestDataFactory.FECHA_TOUR.plusDays(1), LocalTime.of(9, 0)));
    }

    @Test
    @DisplayName("Bloquea la fila de la salida al buscarla para actualizar cupos")
    void findByIdForUpdate() throws NoSuchMethodException {
        Tour tour = tour("bloqueo");
        TourSchedule salida = TestDataFactory.schedule(entityManager, tour, LocalTime.of(9, 0), 10);

        Optional<TourSchedule> bloqueada = repository.findByIdForUpdate(salida.getId());
        assertTrue(bloqueada.isPresent());
        assertEquals(salida.getId(), bloqueada.get().getId());

        Lock lock = TourScheduleRepository.class.getMethod("findByIdForUpdate", Long.class).getAnnotation(Lock.class);
        assertNotNull(lock);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());

        bloqueada.get().discountSpots(4);
        repository.saveAndFlush(bloqueada.get());
        assertEquals(6, repository.findById(salida.getId()).orElseThrow().getCuposDisponibles());
    }

    @Test
    @DisplayName("Suma los cupos totales de las salidas de una agencia dentro del rango")
    void sumTotalSpotsByAgencyAndDateRange() {
        Agency agency = TestDataFactory.agency(entityManager, "ocupacion");
        Agency otra = TestDataFactory.agency(entityManager, "otra-ocupacion");
        ExperienceType type = TestDataFactory.experienceType(entityManager, "Playa");
        Tour tour = TestDataFactory.tour(entityManager, agency, type, "Cupos");
        Tour tourOtro = TestDataFactory.tour(entityManager, otra, type, "CuposOtro");

        TestDataFactory.schedule(entityManager, tour, LocalTime.of(9, 0), 10);
        TestDataFactory.schedule(entityManager, tour, LocalTime.of(11, 0), 5);
        TestDataFactory.schedule(entityManager, tour, LocalTime.of(11, 0),
                TestDataFactory.FECHA_TOUR.plusDays(5), 20);
        TestDataFactory.schedule(entityManager, tourOtro, LocalTime.of(9, 0), 30);

        Long total = repository.sumTotalSpotsByAgencyAndDateRange(agency.getId(),
                TestDataFactory.FECHA_TOUR, TestDataFactory.FECHA_TOUR.plusDays(1));

        assertEquals(15L, total);
        assertEquals(0L, repository.sumTotalSpotsByAgencyAndDateRange(otra.getId(),
                TestDataFactory.FECHA_TOUR.minusDays(10), TestDataFactory.FECHA_TOUR.minusDays(1)));
    }

    @Test
    @DisplayName("Detecta traslape de salidas asignadas a un guía usando la duración del tour")
    void existsOverlappingScheduleForGuide() {
        Agency agency = TestDataFactory.agency(entityManager, "guias-salidas");
        ExperienceType type = TestDataFactory.experienceType(entityManager, "Aventura");
        Tour tour = TestDataFactory.tour(entityManager, agency, type, "Duracion");
        Guide guia = TestDataFactory.guide(entityManager, agency, "asignado");

        TestDataFactory.schedule(entityManager, tour, guia, TestDataFactory.FECHA_TOUR,
                LocalTime.of(9, 0), 10);

        assertEquals(1, repository.findByGuiaIdAndFechaTourInOrderByHoraInicioAsc(
                guia.getId(), List.of(TestDataFactory.FECHA_TOUR)).size());

        assertTrue(repository.existsOverlappingScheduleForGuide(guia.getId(),
                LocalDateTime.of(TestDataFactory.FECHA_TOUR, LocalTime.of(10, 0)),
                LocalDateTime.of(TestDataFactory.FECHA_TOUR, LocalTime.of(12, 0))));
        assertFalse(repository.existsOverlappingScheduleForGuide(guia.getId(),
                LocalDateTime.of(TestDataFactory.FECHA_TOUR, LocalTime.of(11, 30)),
                LocalDateTime.of(TestDataFactory.FECHA_TOUR, LocalTime.of(13, 0))));
        assertFalse(repository.existsOverlappingScheduleForGuide(guia.getId(),
                LocalDateTime.of(TestDataFactory.FECHA_TOUR, LocalTime.of(7, 0)),
                LocalDateTime.of(TestDataFactory.FECHA_TOUR, LocalTime.of(9, 0))));
        assertFalse(repository.existsOverlappingScheduleForGuide(null,
                LocalDateTime.of(TestDataFactory.FECHA_TOUR, LocalTime.of(10, 0)),
                LocalDateTime.of(TestDataFactory.FECHA_TOUR, LocalTime.of(12, 0))));
    }

    private Tour tour(String key) {
        Agency agency = TestDataFactory.agency(entityManager, key);
        ExperienceType type = TestDataFactory.experienceType(entityManager, "Tipo " + key);
        return TestDataFactory.tour(entityManager, agency, type, key);
    }
}
