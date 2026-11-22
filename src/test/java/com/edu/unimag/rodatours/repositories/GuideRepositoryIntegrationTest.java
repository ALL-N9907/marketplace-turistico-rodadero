package com.edu.unimag.rodatours.repositories;

import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.ExperienceType;
import com.edu.unimag.rodatours.domain.entities.Guide;
import com.edu.unimag.rodatours.domain.entities.Tour;
import com.edu.unimag.rodatours.domain.entities.TourSchedule;
import com.edu.unimag.rodatours.domain.entities.enums.GuideStatus;
import com.edu.unimag.rodatours.domain.repositories.GuideRepository;
import com.edu.unimag.rodatours.domain.repositories.TourScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GuideRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

    @Autowired
    private GuideRepository repository;

    @Autowired
    private TourScheduleRepository scheduleRepository;

    @Test
    @DisplayName("Busca guías por documento, registro y correo electrónico")
    void findByIdentificadores() {
        Agency agency = TestDataFactory.agency(entityManager, "guia-id");
        Guide guia = TestDataFactory.guide(entityManager, agency, "uno");

        assertEquals(guia.getId(), repository.findByDocumentoIdentidad("CC-uno").orElseThrow().getId());
        assertEquals(guia.getId(), repository.findByRegistroGuia("RG-uno").orElseThrow().getId());
        assertEquals(guia.getId(), repository.findByCorreoElectronicoIgnoreCase(
                "GUIA.UNO@EXAMPLE.COM").orElseThrow().getId());
        assertTrue(repository.existsByDocumentoIdentidad("CC-uno"));
        assertTrue(repository.existsByRegistroGuia("RG-uno"));
        assertTrue(repository.existsByCorreoElectronicoIgnoreCase("guia.uno@example.com"));
        assertTrue(repository.findByRegistroGuia("RG-otro").isEmpty());
    }

    @Test
    @DisplayName("Filtra guías de una agencia por estado")
    void findByAgenciaIdAndEstado() {
        Agency agency = TestDataFactory.agency(entityManager, "guia-agencia");
        Agency otra = TestDataFactory.agency(entityManager, "guia-otra");

        TestDataFactory.guide(entityManager, agency, "activo", GuideStatus.ACTIVE);
        TestDataFactory.guide(entityManager, agency, "inactivo", GuideStatus.INACTIVE);
        TestDataFactory.guide(entityManager, otra, "externo", GuideStatus.ACTIVE);

        List<Guide> activos = repository.findByAgenciaIdAndEstado(agency.getId(), GuideStatus.ACTIVE);
        assertEquals(1, activos.size());
        assertEquals("Guía activo", activos.getFirst().getNombreCompleto());
        assertEquals(1, repository.findByAgenciaIdAndEstado(agency.getId(), GuideStatus.INACTIVE).size());
        assertEquals(1, repository.findByAgenciaIdAndEstado(otra.getId(), GuideStatus.ACTIVE).size());
    }

    @Test
    @DisplayName("Obtiene las salidas asignadas a un guía en las fechas indicadas")
    void findByGuiaIdAndFechaTourInOrderByHoraInicioAsc() {
        Agency agency = TestDataFactory.agency(entityManager, "guia-horarios");
        ExperienceType type = TestDataFactory.experienceType(entityManager, "Aventura");
        Tour tour = TestDataFactory.tour(entityManager, agency, type, "Guiado");
        Guide guia = TestDataFactory.guide(entityManager, agency, "horarios");

        TestDataFactory.schedule(entityManager, tour, guia, TestDataFactory.FECHA_TOUR,
                LocalTime.of(15, 0), 10);
        TourSchedule temprano = TestDataFactory.schedule(entityManager, tour, guia,
                TestDataFactory.FECHA_TOUR, LocalTime.of(9, 0), 10);
        TestDataFactory.schedule(entityManager, tour, guia,
                TestDataFactory.FECHA_TOUR.plusDays(3), LocalTime.of(9, 0), 10);

        List<TourSchedule> salidas = scheduleRepository.findByGuiaIdAndFechaTourInOrderByHoraInicioAsc(
                guia.getId(), List.of(TestDataFactory.FECHA_TOUR));

        assertEquals(2, salidas.size());
        assertEquals(temprano.getId(), salidas.getFirst().getId());
    }
}
