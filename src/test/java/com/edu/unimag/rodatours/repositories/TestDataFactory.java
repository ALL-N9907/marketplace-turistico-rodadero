package com.edu.unimag.rodatours.repositories;

import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.ExperienceType;
import com.edu.unimag.rodatours.domain.entities.Guide;
import com.edu.unimag.rodatours.domain.entities.Reservation;
import com.edu.unimag.rodatours.domain.entities.Review;
import com.edu.unimag.rodatours.domain.entities.Tour;
import com.edu.unimag.rodatours.domain.entities.TourSchedule;
import com.edu.unimag.rodatours.domain.entities.Tourist;
import com.edu.unimag.rodatours.domain.entities.enums.AgencyStatus;
import com.edu.unimag.rodatours.domain.entities.enums.DifficultyLevel;
import com.edu.unimag.rodatours.domain.entities.enums.GuideStatus;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.entities.enums.TourStatus;
import com.edu.unimag.rodatours.domain.entities.enums.TouristStatus;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Datos mínimos válidos para sembrar el modelo en los tests de repositorio.
 */
public final class TestDataFactory {

    public static final LocalDate FECHA_TOUR = LocalDate.of(2026, 12, 10);

    private TestDataFactory() {
    }

    public static Agency agency(TestEntityManager em, String key) {
        return agency(em, key, AgencyStatus.ACTIVE);
    }

    public static Agency agency(TestEntityManager em, String key, AgencyStatus status) {
        return em.persistAndFlush(Agency.builder()
                .nombreComercial("Agencia " + key)
                .nitORegistro("NIT-" + key)
                .descripcion("Descripción de " + key)
                .direccion("Calle " + key + " # 1-2, El Rodadero")
                .telefonoContacto("3001000001")
                .estado(status)
                .build());
    }

    public static ExperienceType experienceType(TestEntityManager em, String key) {
        return em.persistAndFlush(ExperienceType.builder()
                .nombre("Tipo " + key)
                .descripcion("Experiencias de " + key)
                .build());
    }

    public static Tour tour(TestEntityManager em, Agency agency, ExperienceType type, String key) {
        return tour(em, agency, type, key, TourStatus.ACTIVE, "Destino " + key);
    }

    public static Tour tour(TestEntityManager em, Agency agency, ExperienceType type, String key,
                            TourStatus status, String destino) {
        return em.persistAndFlush(Tour.builder()
                .agencia(agency)
                .tipoExperiencia(type)
                .titulo("Tour " + key)
                .descripcion("Descripción del tour " + key)
                .destino(destino)
                .precio(new BigDecimal("100000.00"))
                .duracionHoras(new BigDecimal("2.5"))
                .nivelDificultad(DifficultyLevel.BAJO)
                .serviciosIncluidos("Guía y transporte")
                .estado(status)
                .build());
    }

    public static TourSchedule schedule(TestEntityManager em, Tour tour, LocalTime hora, int cupos) {
        return schedule(em, tour, null, FECHA_TOUR, hora, cupos);
    }

    public static TourSchedule schedule(TestEntityManager em, Tour tour, LocalTime hora,
                                        LocalDate fecha, int cupos) {
        return schedule(em, tour, null, fecha, hora, cupos);
    }

    public static TourSchedule schedule(TestEntityManager em, Tour tour, Guide guia,
                                        LocalDate fecha, LocalTime hora, int cupos) {
        return schedule(em, tour, guia, fecha, hora, cupos, cupos);
    }

    public static TourSchedule schedule(TestEntityManager em, Tour tour, Guide guia,
                                        LocalDate fecha, LocalTime hora,
                                        int cuposTotales, int cuposDisponibles) {
        return em.persistAndFlush(TourSchedule.builder()
                .tour(tour)
                .guia(guia)
                .fechaTour(fecha)
                .horaInicio(hora)
                .cuposTotales(cuposTotales)
                .cuposDisponibles(cuposDisponibles)
                .build());
    }

    public static Tourist tourist(TestEntityManager em, String key) {
        return tourist(em, key, TouristStatus.ACTIVE);
    }

    public static Tourist tourist(TestEntityManager em, String key, TouristStatus status) {
        return em.persistAndFlush(Tourist.builder()
                .nombreCompleto("Turista " + key)
                .correoElectronico("turista." + key + "@example.com")
                .contrasena("hash-" + key)
                .telefono("3002000001")
                .estado(status)
                .build());
    }

    public static Guide guide(TestEntityManager em, Agency agency, String key) {
        return guide(em, agency, key, GuideStatus.ACTIVE);
    }

    public static Guide guide(TestEntityManager em, Agency agency, String key, GuideStatus status) {
        return em.persistAndFlush(Guide.builder()
                .agencia(agency)
                .nombreCompleto("Guía " + key)
                .documentoIdentidad("CC-" + key)
                .registroGuia("RG-" + key)
                .correoElectronico("guia." + key + "@example.com")
                .telefono("3003000001")
                .idiomas("Español, inglés")
                .estado(status)
                .build());
    }

    public static Reservation reservation(TestEntityManager em, Tourist tourist, TourSchedule schedule,
                                          ReservationStatus status, LocalDateTime inicio, int personas) {
        return reservation(em, tourist, schedule, status, inicio, inicio.plusHours(2), personas, null);
    }

    public static Reservation reservation(TestEntityManager em, Tourist tourist, TourSchedule schedule,
                                          ReservationStatus status, LocalDateTime inicio, LocalDateTime fin,
                                          int personas) {
        return reservation(em, tourist, schedule, status, inicio, fin, personas, null);
    }

    public static Reservation reservation(TestEntityManager em, Tourist tourist, TourSchedule schedule,
                                          ReservationStatus status, LocalDateTime inicio, LocalDateTime fin,
                                          int personas, LocalDateTime fechaReserva) {
        return em.persistAndFlush(Reservation.builder()
                .turista(tourist)
                .horario(schedule)
                .fechaReserva(fechaReserva)
                .fechaInicio(inicio)
                .fechaFin(fin)
                .cantidadPersonas(personas)
                .precioTotal(BigDecimal.valueOf(100000L * personas))
                .estado(status)
                .codigoQr(UUID.randomUUID().toString())
                .build());
    }

    public static Review review(TestEntityManager em, Reservation reservation, int calificacion) {
        return em.persistAndFlush(Review.builder()
                .reserva(reservation)
                .calificacion(calificacion)
                .comentario("Excelente experiencia")
                .build());
    }
}
