package com.edu.unimag.rodatours.history;

import com.edu.unimag.rodatours.api.dtos.request.CreateReservationRequest;
import com.edu.unimag.rodatours.domain.entities.*;
import com.edu.unimag.rodatours.domain.entities.enums.DifficultyLevel;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.repositories.TestPostgresContainer;
import com.edu.unimag.rodatours.services.ReservationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ReservationHistoryTransactionIntegrationTest {
    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) { TestPostgresContainer.register(registry); }

    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ReservationService service;
    @Autowired ReservationRepository reservations;
    @Autowired ReservationHistoryRepository history;

    @Test
    void reservationAndHistoryAreRolledBackTogether() {
        var id = new AtomicReference<Long>();
        var tx = new TransactionTemplate(transactions);
        assertThrows(DeliberateRollback.class, () -> tx.executeWithoutResult(status -> {
            String key = UUID.randomUUID().toString();
            var agency = Agency.builder().nombreComercial("Agencia prueba").nitORegistro(key.substring(0, 20))
                    .direccion("Rodadero").telefonoContacto("3001234567").build();
            em.persist(agency);
            var type = ExperienceType.builder().nombre(key).build();
            em.persist(type);
            var tour = Tour.builder().agencia(agency).tipoExperiencia(type).titulo("Minca").descripcion("Prueba")
                    .destino("Minca").precio(new BigDecimal("100000")).duracionHoras(new BigDecimal("2"))
                    .nivelDificultad(DifficultyLevel.BAJO).build();
            em.persist(tour);
            var tourist = Tourist.builder().nombreCompleto("Prueba").correoElectronico(key + "@example.com")
                    .contrasena("hash-only-for-test").build();
            em.persist(tourist);
            var schedule = TourSchedule.builder().tour(tour).fechaTour(LocalDate.now().plusDays(2))
                    .horaInicio(LocalTime.NOON).cuposTotales(10).cuposDisponibles(10).build();
            em.persist(schedule);
            em.flush();
            id.set(service.create(new CreateReservationRequest(tourist.getId(), schedule.getId(), 1, null)).id());
            em.flush();
            assertEquals(1, history.findByReservationId(id.get(), PageRequest.of(0, 20)).getTotalElements());
            throw new DeliberateRollback();
        }));
        assertNotNull(id.get());
        assertFalse(reservations.existsById(id.get()));
        assertEquals(0, history.findByReservationId(id.get(), PageRequest.of(0, 20)).getTotalElements());
    }

    private static class DeliberateRollback extends RuntimeException {}
}
