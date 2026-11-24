package com.edu.unimag.rodatours.security;

import com.edu.unimag.rodatours.api.dtos.request.CreateReservationRequest;
import com.edu.unimag.rodatours.domain.entities.*;
import com.edu.unimag.rodatours.domain.entities.enums.DifficultyLevel;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.repositories.TestPostgresContainer;
import com.edu.unimag.rodatours.services.ReservationService;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ReservationConcurrencyIntegrationTest {
    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) { TestPostgresContainer.register(registry); }

    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired ReservationService service;
    @Autowired ReservationRepository reservations;
    private Long touristId;
    private Long firstSchedule;
    private Long secondSchedule;
    private Long agencyId;
    private Long typeId;
    private List<Long> tours;

    private <T> T persist(T entity) { em.persist(entity); return entity; }

    @BeforeEach
    void setup() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            String key = UUID.randomUUID().toString().substring(0, 8);
            var agency = persist(Agency.builder().nombreComercial("Concurrency " + key).nitORegistro("NIT-" + key)
                    .direccion("Rodadero").telefonoContacto("3001234567").build());
            var type = persist(ExperienceType.builder().nombre("Concurrency " + key).build());
            var tourist = persist(Tourist.builder().nombreCompleto("Concurrency " + key)
                    .correoElectronico(key + "@example.com").contrasena("unused-test-hash").build());
            var tour1 = tour(agency, type, "Uno");
            var tour2 = tour(agency, type, "Dos");
            var schedule1 = schedule(tour1);
            var schedule2 = schedule(tour2);
            em.flush();
            touristId = tourist.getId();
            firstSchedule = schedule1.getId();
            secondSchedule = schedule2.getId();
            agencyId = agency.getId();
            typeId = type.getId();
            tours = List.of(tour1.getId(), tour2.getId());
        });
    }

    private Tour tour(Agency agency, ExperienceType type, String title) {
        return persist(Tour.builder().agencia(agency).tipoExperiencia(type).titulo(title).descripcion("Prueba")
                .destino("Minca").precio(new BigDecimal("100000")).duracionHoras(new BigDecimal("2"))
                .nivelDificultad(DifficultyLevel.BAJO).build());
    }

    private TourSchedule schedule(Tour tour) {
        return persist(TourSchedule.builder().tour(tour).fechaTour(LocalDate.now().plusDays(10))
                .horaInicio(LocalTime.of(8, 0)).cuposTotales(10).cuposDisponibles(10).build());
    }

    @AfterEach
    void cleanup() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            em.createQuery("delete from Notification n where n.reservationId in (select r.id from Reservation r where r.turista.id = :id)")
                    .setParameter("id", touristId).executeUpdate();
            em.createQuery("delete from ReservationHistoryEntry h where h.reservation.turista.id = :id")
                    .setParameter("id", touristId).executeUpdate();
            em.createQuery("delete from Reservation r where r.turista.id = :id").setParameter("id", touristId).executeUpdate();
            em.createQuery("delete from TourSchedule s where s.id in :ids")
                    .setParameter("ids", List.of(firstSchedule, secondSchedule)).executeUpdate();
            em.createQuery("delete from Tour t where t.id in :ids").setParameter("ids", tours).executeUpdate();
            em.createQuery("delete from Tourist t where t.id = :id").setParameter("id", touristId).executeUpdate();
            em.createQuery("delete from Agency a where a.id = :id").setParameter("id", agencyId).executeUpdate();
            em.createQuery("delete from ExperienceType t where t.id = :id").setParameter("id", typeId).executeUpdate();
        });
    }

    private Throwable call(CountDownLatch start, Runnable operation) throws InterruptedException {
        if (!start.await(10, TimeUnit.SECONDS)) { throw new AssertionError("No se inició la prueba concurrente."); }
        try { operation.run(); return null; }
        catch (RuntimeException exception) { return exception; }
    }

    @Test
    void committedReservationPersistsNotificationAtomically() {
        Long id = service.create(new CreateReservationRequest(touristId, firstSchedule, 1, null)).id();
        new TransactionTemplate(manager).executeWithoutResult(status ->
                assertEquals(1L, em.createQuery("select count(n) from Notification n where n.reservationId = :id", Long.class)
                        .setParameter("id", id).getSingleResult()));
    }

    @Test
    void rolledBackReservationDoesNotProduceNotifications() {
        Long id = new TransactionTemplate(manager).execute(status -> {
            Long reservationId = service.create(new CreateReservationRequest(touristId, firstSchedule, 1, null)).id();
            status.setRollbackOnly();
            return reservationId;
        });
        new TransactionTemplate(manager).executeWithoutResult(status ->
                assertEquals(0L, em.createQuery("select count(n) from Notification n where n.reservationId = :id", Long.class)
                        .setParameter("id", id).getSingleResult()));
    }

    @Test
    void sameTouristCannotCreateOverlappingReservationsOnDifferentSchedules() throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> call(start, () -> service.create(new CreateReservationRequest(touristId, firstSchedule, 1, null))));
            var second = executor.submit(() -> call(start, () -> service.create(new CreateReservationRequest(touristId, secondSchedule, 1, null))));
            start.countDown();
            Throwable a = first.get(20, TimeUnit.SECONDS);
            Throwable b = second.get(20, TimeUnit.SECONDS);
            assertTrue((a == null) != (b == null), "Sólo una reserva debe crearse.");
            assertInstanceOf(ConflictException.class, a == null ? b : a);
            new TransactionTemplate(manager).executeWithoutResult(status ->
                    assertEquals(1, reservations.findByTuristaIdOrderByFechaReservaDesc(touristId).size()));
        }
    }

    @Test
    void concurrentConfirmationDiscountsSpotsOnlyOnce() throws Exception {
        Long id = service.create(new CreateReservationRequest(touristId, firstSchedule, 1, null)).id();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> call(start, () -> service.confirm(id)));
            var second = executor.submit(() -> call(start, () -> service.confirm(id)));
            start.countDown();
            Throwable a = first.get(20, TimeUnit.SECONDS);
            Throwable b = second.get(20, TimeUnit.SECONDS);
            assertTrue((a == null) != (b == null), "Sólo una confirmación debe tener éxito.");
            Throwable failure = a == null ? b : a;
            assertTrue(failure instanceof ConflictException || failure instanceof OptimisticLockingFailureException,
                    "Se esperaba conflicto de estado o versión, no: " + failure);
            new TransactionTemplate(manager).executeWithoutResult(status ->
                    assertEquals(9, em.find(TourSchedule.class, firstSchedule).getCuposDisponibles()));
        }
    }
}
