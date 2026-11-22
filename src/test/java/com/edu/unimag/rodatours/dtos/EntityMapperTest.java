package com.edu.unimag.rodatours.dtos;

import com.edu.unimag.rodatours.services.mapper.AgencyMapper;
import com.edu.unimag.rodatours.services.mapper.AvailabilityMapper;
import com.edu.unimag.rodatours.services.mapper.ExperienceTypeMapper;
import com.edu.unimag.rodatours.services.mapper.GuideMapper;
import com.edu.unimag.rodatours.services.mapper.ReservationMapper;
import com.edu.unimag.rodatours.services.mapper.ReviewMapper;
import com.edu.unimag.rodatours.services.mapper.TourMapper;
import com.edu.unimag.rodatours.services.mapper.TourScheduleMapper;
import com.edu.unimag.rodatours.services.mapper.TouristMapper;
import com.edu.unimag.rodatours.api.dtos.request.CreateAgencyRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateExperienceTypeRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateGuideRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateReservationRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateReviewRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateTourRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateTourScheduleRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateTouristRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateAgencyRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateExperienceTypeRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateGuideRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateReservationRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateReviewRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTourRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTourScheduleRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTouristRequest;
import com.edu.unimag.rodatours.api.dtos.response.AvailabilitySlotResponse;
import com.edu.unimag.rodatours.api.dtos.response.ReservationResponse;
import com.edu.unimag.rodatours.api.dtos.response.TouristResponse;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class EntityMapperTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 12, 10);
    private static final LocalTime HORA = LocalTime.of(9, 0);

    @Test
    @DisplayName("TouristMapper convierte en ambos sentidos sin exponer la contraseña")
    void mapearTurista() {
        Tourist entity = TouristMapper.toEntity(new CreateTouristRequest(
                "Ana Pérez", "ana@example.com", "hash-secreto", "3001234567", null));

        assertNotNull(entity);
        assertEquals("Ana Pérez", entity.getNombreCompleto());
        assertEquals("ana@example.com", entity.getCorreoElectronico());
        assertEquals("hash-secreto", entity.getContrasena());
        assertEquals(TouristStatus.ACTIVE, entity.getEstado());

        entity.setId(1L);
        entity.setFechaCreacion(LocalDateTime.of(2026, 11, 1, 10, 0));
        TouristResponse response = TouristMapper.toResponse(entity);
        assertEquals(1L, response.id());
        assertEquals("Ana Pérez", response.nombreCompleto());
        assertEquals("ana@example.com", response.correoElectronico());
        assertEquals(TouristStatus.ACTIVE, response.estado());
        assertFalse(Arrays.stream(TouristResponse.class.getRecordComponents())
                .anyMatch(componente -> componente.getName().equals("contrasena")));

        LocalDateTime fechaCreacion = entity.getFechaCreacion();
        TouristMapper.applyUpdate(new UpdateTouristRequest(
                "Ana M. Pérez", "ana.nueva@example.com", "3009999999", TouristStatus.INACTIVE), entity);
        assertEquals("Ana M. Pérez", entity.getNombreCompleto());
        assertEquals("ana.nueva@example.com", entity.getCorreoElectronico());
        assertEquals(TouristStatus.INACTIVE, entity.getEstado());
        assertEquals(fechaCreacion, entity.getFechaCreacion());
        assertEquals("hash-secreto", entity.getContrasena());

        TouristMapper.applyUpdate(new UpdateTouristRequest("Ana", "ana@example.com", null, null), entity);
        assertEquals(TouristStatus.INACTIVE, entity.getEstado(), "Un estado nulo no debe sobrescribir el actual");
    }

    @Test
    @DisplayName("AgencyMapper y ExperienceTypeMapper aplican actualizaciones sin tocar el id")
    void mapearAgenciaYTipo() {
        Agency agencia = AgencyMapper.toEntity(new CreateAgencyRequest(
                "Rodadero Tours", "NIT-1", "Descripción", "Calle 1", "3001000001", AgencyStatus.INACTIVE));
        agencia.setId(2L);
        assertEquals(AgencyStatus.INACTIVE, agencia.getEstado());
        assertEquals(2L, AgencyMapper.toResponse(agencia).id());
        assertEquals("NIT-1", AgencyMapper.toResponse(agencia).nitORegistro());

        AgencyMapper.applyUpdate(new UpdateAgencyRequest(
                "Nuevo nombre", "NIT-1", null, "Calle 2", "3001000002", null), agencia);
        assertEquals("Nuevo nombre", agencia.getNombreComercial());
        assertEquals("Calle 2", agencia.getDireccion());
        assertEquals(AgencyStatus.INACTIVE, agencia.getEstado());
        assertEquals(2L, agencia.getId());

        ExperienceType tipo = ExperienceTypeMapper.toEntity(
                new CreateExperienceTypeRequest("Aventura", "Excursiones"));
        tipo.setId(3L);
        assertEquals("Aventura", ExperienceTypeMapper.toResponse(tipo).nombre());
        ExperienceTypeMapper.applyUpdate(new UpdateExperienceTypeRequest("Naturaleza", null), tipo);
        assertEquals("Naturaleza", tipo.getNombre());
        assertNull(tipo.getDescripcion());
    }

    @Test
    @DisplayName("TourMapper usa las relaciones resueltas y respeta un estado nulo en la actualización")
    void mapearTour() {
        Agency agencia = Agency.builder().id(1L).build();
        ExperienceType tipo = ExperienceType.builder().id(2L).build();
        Tour tour = TourMapper.toEntity(new CreateTourRequest(
                1L, 2L, "Tour Minca", "Descripción", "Minca",
                new BigDecimal("100000.00"), new BigDecimal("2.5"),
                DifficultyLevel.MEDIO, "Guía y transporte", null), agencia, tipo);

        assertNotNull(tour);
        assertSame(agencia, tour.getAgencia());
        assertSame(tipo, tour.getTipoExperiencia());
        assertEquals(TourStatus.ACTIVE, tour.getEstado());

        tour.setId(4L);
        var response = TourMapper.toResponse(tour);
        assertEquals(1L, response.agenciaId());
        assertEquals(2L, response.tipoExperienciaId());
        assertEquals("Minca", response.destino());
        assertEquals(new BigDecimal("2.5"), response.duracionHoras());

        tour.setEstado(TourStatus.INACTIVE);
        Agency otraAgencia = Agency.builder().id(5L).build();
        TourMapper.applyUpdate(new UpdateTourRequest(
                5L, 2L, "Otro título", "Otra descripción", "Tayrona",
                new BigDecimal("120000.00"), new BigDecimal("3.0"),
                DifficultyLevel.ALTO, "Transporte", null), tour, otraAgencia, tipo);
        assertEquals(5L, tour.getAgencia().getId());
        assertEquals("Tayrona", tour.getDestino());
        assertEquals(TourStatus.INACTIVE, tour.getEstado());
    }

    @Test
    @DisplayName("TourScheduleMapper inicializa cupos y calcula la hora de fin desde la duración del tour")
    void mapearHorario() {
        Tour tour = Tour.builder()
                .id(6L)
                .duracionHoras(new BigDecimal("2.5"))
                .build();
        Guide guia = Guide.builder().id(7L).build();

        TourSchedule schedule = TourScheduleMapper.toEntity(
                new CreateTourScheduleRequest(7L, FECHA, HORA, 10), tour, guia);
        assertNotNull(schedule);
        assertSame(tour, schedule.getTour());
        assertSame(guia, schedule.getGuia());
        assertEquals(10, schedule.getCuposTotales());
        assertEquals(10, schedule.getCuposDisponibles());

        schedule.setId(8L);
        var response = TourScheduleMapper.toResponse(schedule);
        assertEquals(6L, response.tourId());
        assertEquals(7L, response.guiaId());
        assertEquals(LocalDateTime.of(FECHA, HORA), response.fechaTour().atTime(response.horaInicio()));
        assertEquals(LocalDateTime.of(2026, 12, 10, 11, 30), response.fechaFin());

        schedule.discountSpots(4);
        Guide nuevoGuia = Guide.builder().id(9L).build();
        TourScheduleMapper.applyUpdate(
                new UpdateTourScheduleRequest(9L, FECHA.plusDays(1), LocalTime.of(10, 0), 20),
                schedule, nuevoGuia);
        assertEquals(9L, schedule.getGuia().getId());
        assertEquals(LocalTime.of(10, 0), schedule.getHoraInicio());
        assertEquals(20, schedule.getCuposTotales());
        assertEquals(6, schedule.getCuposDisponibles(), "La actualización no debe alterar los cupos tomados");
    }

    @Test
    @DisplayName("ReservationMapper recibe los valores calculados y el update solo cambia observaciones")
    void mapearReserva() {
        Tourist turista = Tourist.builder().id(1L).build();
        TourSchedule horario = TourSchedule.builder().id(2L).build();
        LocalDateTime inicio = LocalDateTime.of(FECHA, HORA);
        LocalDateTime fin = inicio.plusHours(2);
        BigDecimal precio = new BigDecimal("200000.00");

        Reservation entity = ReservationMapper.toEntity(
                new CreateReservationRequest(1L, 2L, 2, "Nota inicial"),
                turista, horario, inicio, fin, precio, "QR-123");

        assertNotNull(entity);
        assertSame(turista, entity.getTurista());
        assertSame(horario, entity.getHorario());
        assertEquals(inicio, entity.getFechaInicio());
        assertEquals(fin, entity.getFechaFin());
        assertEquals(precio, entity.getPrecioTotal());
        assertEquals("QR-123", entity.getCodigoQr());
        assertEquals(ReservationStatus.SCHEDULED, entity.getEstado());

        entity.setId(10L);
        entity.setFechaReserva(LocalDateTime.of(2026, 11, 1, 12, 0));
        entity.setFechaModificacion(LocalDateTime.of(2026, 11, 1, 12, 0));
        entity.confirm();
        ReservationResponse response = ReservationMapper.toResponse(entity);
        assertEquals(1L, response.turistaId());
        assertEquals(2L, response.horarioId());
        assertEquals("QR-123", response.codigoQr());
        assertEquals(ReservationStatus.CONFIRMED, response.estado());

        LocalDateTime modificacion = entity.getFechaModificacion();
        ReservationMapper.applyUpdate(new UpdateReservationRequest("Nota actualizada"), entity);
        assertEquals("Nota actualizada", entity.getObservaciones());
        assertEquals(ReservationStatus.CONFIRMED, entity.getEstado());
        assertEquals(precio, entity.getPrecioTotal());
        assertEquals("QR-123", entity.getCodigoQr());
        assertEquals(modificacion, entity.getFechaModificacion());
    }

    @Test
    @DisplayName("GuideMapper y ReviewMapper exponen las relaciones como identificadores")
    void mapearGuiaYResena() {
        Agency agencia = Agency.builder().id(1L).build();
        Guide guia = GuideMapper.toEntity(new CreateGuideRequest(
                1L, "Guía Uno", "CC-1", "RG-1", "guia@example.com", "3002000001", "Español", null), agencia);
        guia.setId(11L);
        assertEquals(1L, GuideMapper.toResponse(guia).agenciaId());
        assertEquals(GuideStatus.ACTIVE, guia.getEstado());

        Agency otraAgencia = Agency.builder().id(2L).build();
        GuideMapper.applyUpdate(new UpdateGuideRequest(
                2L, "Guía Dos", "CC-2", "RG-2", "nuevo@example.com", "3002000002", "Inglés",
                GuideStatus.INACTIVE), guia, otraAgencia);
        assertEquals(2L, guia.getAgencia().getId());
        assertEquals(GuideStatus.INACTIVE, guia.getEstado());

        Reservation reserva = Reservation.builder().id(12L).build();
        Review review = ReviewMapper.toEntity(new CreateReviewRequest(12L, 5, "Excelente"), reserva);
        review.setId(13L);
        assertEquals(12L, ReviewMapper.toResponse(review).reservaId());
        assertEquals(5, ReviewMapper.toResponse(review).calificacion());

        ReviewMapper.applyUpdate(new UpdateReviewRequest(4, "Buena"), review);
        assertEquals(4, review.getCalificacion());
        assertEquals("Buena", review.getComentario());
    }

    @Test
    @DisplayName("AvailabilityMapper convierte una salida en una franja con cupos reales")
    void mapearDisponibilidad() {
        Tour tour = Tour.builder().id(6L).duracionHoras(new BigDecimal("3.0")).build();
        TourSchedule schedule = TourSchedule.builder()
                .id(8L)
                .tour(tour)
                .fechaTour(FECHA)
                .horaInicio(HORA)
                .cuposTotales(10)
                .cuposDisponibles(3)
                .build();

        AvailabilitySlotResponse slot = AvailabilityMapper.toResponse(schedule);
        assertEquals(8L, slot.horarioId());
        assertEquals(6L, slot.tourId());
        assertEquals(FECHA, slot.fechaTour());
        assertEquals(HORA, slot.horaInicio());
        assertEquals(LocalDateTime.of(2026, 12, 10, 12, 0), slot.fechaFin());
        assertEquals(10, slot.cuposTotales());
        assertEquals(3, slot.cuposDisponibles());
    }

    @Test
    @DisplayName("Los mappers devuelven null cuando reciben null")
    void mappersNullSafe() {
        assertNull(TouristMapper.toEntity(null));
        assertNull(TouristMapper.toResponse(null));
        assertNull(AgencyMapper.toEntity(null));
        assertNull(ExperienceTypeMapper.toResponse(null));
        assertNull(TourMapper.toEntity(null, null, null));
        assertNull(TourScheduleMapper.toResponse(null));
        assertNull(ReservationMapper.toEntity(null, null, null, null, null, null, null));
        assertNull(ReservationMapper.toResponse(null));
        assertNull(GuideMapper.toEntity(null, null));
        assertNull(ReviewMapper.toResponse(null));
        assertNull(AvailabilityMapper.toResponse(null));

        Tour tour = Tour.builder().build();
        TourStatus estado = tour.getEstado();
        TourMapper.applyUpdate(null, tour, null, null);
        assertEquals(estado, tour.getEstado());
    }
}
