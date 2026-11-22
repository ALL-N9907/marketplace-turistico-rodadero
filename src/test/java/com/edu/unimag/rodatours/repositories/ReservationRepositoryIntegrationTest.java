package com.edu.unimag.rodatours.repositories;

import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.ExperienceType;
import com.edu.unimag.rodatours.domain.entities.Reservation;
import com.edu.unimag.rodatours.domain.entities.Tour;
import com.edu.unimag.rodatours.domain.entities.TourSchedule;
import com.edu.unimag.rodatours.domain.entities.Tourist;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.domain.repositories.projections.AgencyOccupancyProjection;
import com.edu.unimag.rodatours.domain.repositories.projections.AgencyProductivityProjection;
import com.edu.unimag.rodatours.domain.repositories.projections.ExperienceTypeCancellationProjection;
import com.edu.unimag.rodatours.domain.repositories.projections.NoShowTouristProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReservationRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

    private static final List<ReservationStatus> ESTADOS_ACTIVOS =
            List.of(ReservationStatus.SCHEDULED, ReservationStatus.CONFIRMED);
    private static final List<ReservationStatus> ESTADOS_OCUPADOS =
            List.of(ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED);
    private static final LocalDate DESDE = TestDataFactory.FECHA_TOUR;
    private static final LocalDate HASTA = TestDataFactory.FECHA_TOUR.plusDays(1);

    @Autowired
    private ReservationRepository repository;

    @Test
    @DisplayName("Busca reservas por código QR y por horario")
    void findByCodigoQrYHorario() {
        Reservation reserva = reserva("qr", ReservationStatus.SCHEDULED, LocalTime.of(9, 0), 2);

        assertEquals(reserva.getId(), repository.findByCodigoQr(reserva.getCodigoQr()).orElseThrow().getId());
        assertTrue(repository.findByCodigoQr("no-existe").isEmpty());
        assertEquals(1, repository.findByHorarioId(reserva.getHorario().getId()).size());
    }

    @Test
    @DisplayName("Filtra reservas de un turista por estado")
    void findByTuristaIdAndEstado() {
        TourSchedule schedule = schedule("estados");
        Tourist turista = TestDataFactory.tourist(entityManager, "con-estados");
        TestDataFactory.reservation(entityManager, turista, schedule,
                ReservationStatus.SCHEDULED, inicio(9, 0), 2);
        TestDataFactory.reservation(entityManager, turista, schedule,
                ReservationStatus.CONFIRMED, inicio(11, 0), 2);
        TestDataFactory.reservation(entityManager, turista, schedule,
                ReservationStatus.CANCELLED, inicio(13, 0), 2);
        Tourist otro = TestDataFactory.tourist(entityManager, "otros-estados");
        TestDataFactory.reservation(entityManager, otro, schedule,
                ReservationStatus.CONFIRMED, inicio(15, 0), 1);

        assertEquals(1, repository.findByTuristaIdAndEstado(turista.getId(), ReservationStatus.SCHEDULED).size());
        assertEquals(1, repository.findByTuristaIdAndEstado(turista.getId(), ReservationStatus.CONFIRMED).size());
        assertEquals(1, repository.findByTuristaIdAndEstado(turista.getId(), ReservationStatus.CANCELLED).size());
        assertEquals(3, repository.findByTuristaIdOrderByFechaReservaDesc(turista.getId()).size());
        assertEquals(1, repository.findByTuristaIdAndEstado(otro.getId(), ReservationStatus.CONFIRMED).size());
    }

    @Test
    @DisplayName("Busca reservas por rango de fecha de creación y por rango de fecha de tour")
    void findByRangosDeFechas() {
        TourSchedule schedule = schedule("rangos");
        Tourist turista = TestDataFactory.tourist(entityManager, "rangos");
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 11, 1, 10, 0);
        TestDataFactory.reservation(entityManager, turista, schedule, ReservationStatus.CONFIRMED,
                inicio(9, 0), inicio(11, 0), 2, fechaCreacion);
        TestDataFactory.reservation(entityManager, turista, schedule, ReservationStatus.CONFIRMED,
                inicio(9, 0).plusDays(1), inicio(11, 0).plusDays(1), 2,
                fechaCreacion.plusDays(20));

        assertEquals(1, repository.findByFechaReservaBetween(
                LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 11, 15, 0, 0)).size());
        assertEquals(2, repository.findByFechaReservaBetween(
                fechaCreacion.minusDays(1), fechaCreacion.plusDays(25)).size());
        assertEquals(1, repository.findByFechaInicioBetween(
                inicio(8, 0), inicio(12, 0)).size());
        assertEquals(2, repository.findByFechaInicioBetween(
                DESDE.atStartOfDay(), HASTA.atTime(LocalTime.MAX)).size());
    }

    @Test
    @DisplayName("Detecta traslapes de reservas activas e ignora canceladas y reservas finales")
    void existsOverlappingByTuristaAndStates() {
        TourSchedule schedule = schedule("traslapes");
        Tourist turista = TestDataFactory.tourist(entityManager, "traslapes");
        TestDataFactory.reservation(entityManager, turista, schedule,
                ReservationStatus.CONFIRMED, inicio(10, 0), inicio(12, 0), 2);

        assertTrue(repository.existsOverlappingByTuristaAndStates(turista.getId(), ESTADOS_ACTIVOS,
                inicio(11, 0), inicio(13, 0)));
        assertTrue(repository.existsOverlappingByTuristaAndStates(turista.getId(), ESTADOS_ACTIVOS,
                inicio(9, 0), inicio(10, 30)));
        assertFalse(repository.existsOverlappingByTuristaAndStates(turista.getId(), ESTADOS_ACTIVOS,
                inicio(12, 0), inicio(13, 0)));
        assertFalse(repository.existsOverlappingByTuristaAndStates(turista.getId(), ESTADOS_ACTIVOS,
                inicio(13, 0), inicio(14, 0)));
        assertEquals(0L, repository.countOverlappingByTuristaAndStates(turista.getId(),
                List.of(ReservationStatus.CANCELLED), inicio(11, 0), inicio(13, 0)));
    }

    @Test
    @DisplayName("Calcula cupos ocupados y el reporte de ocupación de una agencia")
    void ocupacionDeAgencia() {
        Agency agency = TestDataFactory.agency(entityManager, "ocupada");
        ExperienceType type = TestDataFactory.experienceType(entityManager, "Playa");
        Tour tour = TestDataFactory.tour(entityManager, agency, type, "Ocupacion");
        TourSchedule schedule = TestDataFactory.schedule(entityManager, tour, LocalTime.of(9, 0), 10);
        TestDataFactory.schedule(entityManager, tour, LocalTime.of(11, 0), 5);
        Tourist turista = TestDataFactory.tourist(entityManager, "ocupantes");

        TestDataFactory.reservation(entityManager, turista, schedule, ReservationStatus.CONFIRMED, inicio(9, 0), 3);
        TestDataFactory.reservation(entityManager, turista, schedule, ReservationStatus.COMPLETED, inicio(11, 0), 2);
        TestDataFactory.reservation(entityManager, turista, schedule, ReservationStatus.CANCELLED, inicio(13, 0), 4);
        TestDataFactory.reservation(entityManager, turista, schedule, ReservationStatus.SCHEDULED, inicio(15, 0), 1);

        assertEquals(5L, repository.sumOccupiedSpotsByAgencyAndDateRange(
                agency.getId(), DESDE, HASTA, ESTADOS_OCUPADOS));

        AgencyOccupancyProjection ocupacion = repository.findAgencyOccupancy(
                agency.getId(), DESDE, HASTA, ESTADOS_OCUPADOS).orElseThrow();
        assertEquals(agency.getId(), ocupacion.getAgenciaId());
        assertEquals("Agencia ocupada", ocupacion.getNombreComercial());
        assertEquals(15L, ocupacion.getCuposTotales());
        assertEquals(5L, ocupacion.getCuposOcupados());
    }

    @Test
    @DisplayName("Genera el ranking de agencias por tours completados")
    void rankingAgenciesByCompletedTours() {
        Agency primera = TestDataFactory.agency(entityManager, "primera");
        Agency segunda = TestDataFactory.agency(entityManager, "segunda");
        ExperienceType type = TestDataFactory.experienceType(entityManager, "Aventura");
        Tour tourPrimera = TestDataFactory.tour(entityManager, primera, type, "Ranking1");
        Tour tourSegunda = TestDataFactory.tour(entityManager, segunda, type, "Ranking2");
        TourSchedule s1 = TestDataFactory.schedule(entityManager, tourPrimera, LocalTime.of(9, 0), 10);
        TourSchedule s2 = TestDataFactory.schedule(entityManager, tourSegunda, LocalTime.of(9, 0), 10);
        Tourist turista = TestDataFactory.tourist(entityManager, "ranking");

        TestDataFactory.reservation(entityManager, turista, s1, ReservationStatus.COMPLETED, inicio(9, 0), 1);
        TestDataFactory.reservation(entityManager, turista, s1, ReservationStatus.COMPLETED, inicio(11, 0), 1);
        TestDataFactory.reservation(entityManager, turista, s2, ReservationStatus.COMPLETED, inicio(9, 0), 1);
        TestDataFactory.reservation(entityManager, turista, s2, ReservationStatus.CONFIRMED, inicio(11, 0), 1);

        List<AgencyProductivityProjection> ranking = repository.rankingAgenciesByCompletedTours(
                ReservationStatus.COMPLETED, DESDE, HASTA);

        assertEquals(2, ranking.size());
        assertEquals(primera.getId(), ranking.getFirst().getAgenciaId());
        assertEquals(2L, ranking.getFirst().getToursCompletados());
        assertEquals(segunda.getId(), ranking.get(1).getAgenciaId());
        assertEquals(1L, ranking.get(1).getToursCompletados());
    }

    @Test
    @DisplayName("Reporta turistas con más inasistencias en el período")
    void findNoShowTourists() {
        TourSchedule schedule = schedule("no-shows");
        Tourist frecuente = TestDataFactory.tourist(entityManager, "frecuente");
        Tourist ocasional = TestDataFactory.tourist(entityManager, "ocasional");

        TestDataFactory.reservation(entityManager, frecuente, schedule, ReservationStatus.NO_SHOW, inicio(9, 0), 1);
        TestDataFactory.reservation(entityManager, frecuente, schedule, ReservationStatus.NO_SHOW, inicio(11, 0), 1);
        TestDataFactory.reservation(entityManager, ocasional, schedule, ReservationStatus.NO_SHOW, inicio(13, 0), 1);
        TourSchedule fueraDeRango = schedule("fuera-rango", DESDE.plusDays(10));
        TestDataFactory.reservation(entityManager, ocasional, fueraDeRango, ReservationStatus.NO_SHOW,
                LocalDateTime.of(DESDE.plusDays(10), LocalTime.of(9, 0)), 1);

        List<NoShowTouristProjection> noShows = repository.findNoShowTourists(
                ReservationStatus.NO_SHOW, DESDE, HASTA);

        assertEquals(2, noShows.size());
        assertEquals(frecuente.getId(), noShows.getFirst().getTuristaId());
        assertEquals(2L, noShows.getFirst().getCantidadNoShow());
        assertEquals(1L, noShows.get(1).getCantidadNoShow());
    }

    @Test
    @DisplayName("Agrupa cancelaciones e inasistencias por tipo de experiencia y estado")
    void countByExperienceTypeAndState() {
        Agency agency = TestDataFactory.agency(entityManager, "tipos");
        ExperienceType aventura = TestDataFactory.experienceType(entityManager, "Aventura");
        ExperienceType cultural = TestDataFactory.experienceType(entityManager, "Cultural");
        Tour tourAventura = TestDataFactory.tour(entityManager, agency, aventura, "TipoAventura");
        Tour tourCultural = TestDataFactory.tour(entityManager, agency, cultural, "TipoCultural");
        TourSchedule s1 = TestDataFactory.schedule(entityManager, tourAventura, LocalTime.of(9, 0), 10);
        TourSchedule s2 = TestDataFactory.schedule(entityManager, tourCultural, LocalTime.of(9, 0), 10);
        Tourist turista = TestDataFactory.tourist(entityManager, "tipos");

        TestDataFactory.reservation(entityManager, turista, s1, ReservationStatus.CANCELLED, inicio(9, 0), 1);
        TestDataFactory.reservation(entityManager, turista, s1, ReservationStatus.NO_SHOW, inicio(11, 0), 1);
        TestDataFactory.reservation(entityManager, turista, s2, ReservationStatus.CANCELLED, inicio(9, 0), 1);

        List<ExperienceTypeCancellationProjection> filas = repository.countByExperienceTypeAndState(
                List.of(ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW), DESDE, HASTA);

        assertEquals(3, filas.size());
        assertEquals("Tipo Aventura", filas.get(0).getTipoExperiencia());
        assertEquals(ReservationStatus.CANCELLED, filas.get(0).getEstado());
        assertEquals(1L, filas.get(0).getCantidad());
        assertEquals("Tipo Aventura", filas.get(1).getTipoExperiencia());
        assertEquals(ReservationStatus.NO_SHOW, filas.get(1).getEstado());
        assertEquals("Tipo Cultural", filas.get(2).getTipoExperiencia());
    }

    private LocalDateTime inicio(int hora, int minuto) {
        return LocalDateTime.of(DESDE, LocalTime.of(hora, minuto));
    }

    private TourSchedule schedule(String key) {
        return schedule(key, DESDE);
    }

    private TourSchedule schedule(String key, LocalDate fecha) {
        Agency agency = TestDataFactory.agency(entityManager, key);
        ExperienceType type = TestDataFactory.experienceType(entityManager, "Tipo " + key);
        Tour tour = TestDataFactory.tour(entityManager, agency, type, key);
        return TestDataFactory.schedule(entityManager, tour, null, fecha, LocalTime.of(9, 0), 20);
    }

    private Reservation reserva(String key, ReservationStatus status, LocalTime hora, int personas) {
        TourSchedule schedule = schedule(key);
        Tourist turista = TestDataFactory.tourist(entityManager, key);
        return TestDataFactory.reservation(entityManager, turista, schedule, status,
                LocalDateTime.of(DESDE, hora), personas);
    }
}
