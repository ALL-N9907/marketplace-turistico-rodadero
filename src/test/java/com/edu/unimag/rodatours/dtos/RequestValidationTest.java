package com.edu.unimag.rodatours.dtos;

import com.edu.unimag.rodatours.api.dtos.request.CancelReservationRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateAgencyRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateExperienceTypeRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateGuideRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateReservationRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateReviewRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateTourRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateTourScheduleRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateTouristRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateReservationRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateReviewRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTouristRequest;
import com.edu.unimag.rodatours.domain.entities.enums.DifficultyLevel;
import com.edu.unimag.rodatours.domain.entities.enums.TouristStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RequestValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("CreateTouristRequest valida nombre, correo y contraseña")
    void validaCreacionDeTurista() {
        CreateTouristRequest valido = new CreateTouristRequest(
                "Ana Pérez", "ana@example.com", "secreto123", "3001234567", TouristStatus.ACTIVE);
        assertTrue(VALIDATOR.validate(valido).isEmpty());

        assertCampoInvalido(new CreateTouristRequest(
                " ", "ana@example.com", "secreto123", null, null), "nombreCompleto");
        assertCampoInvalido(new CreateTouristRequest(
                "Ana", "correo-invalido", "secreto123", null, null), "correoElectronico");
        assertCampoInvalido(new CreateTouristRequest(
                "Ana", "ana@example.com", " ", null, null), "contrasena");
        assertTrue(VALIDATOR.validate(new CreateTouristRequest(
                "Ana", "ana@example.com", "secreto", null, null)).isEmpty(),
                "El estado y el teléfono son opcionales");
    }

    @Test
    @DisplayName("UpdateTouristRequest valida correo y longitud del nombre")
    void validaActualizacionDeTurista() {
        assertTrue(VALIDATOR.validate(new UpdateTouristRequest(
                "Ana Pérez", "ana@example.com", null, TouristStatus.INACTIVE)).isEmpty());

        assertCampoInvalido(new UpdateTouristRequest(" ", "ana@example.com", null, null), "nombreCompleto");
        assertCampoInvalido(new UpdateTouristRequest("Ana", "correo-invalido", null, null), "correoElectronico");
    }

    @Test
    @DisplayName("CreateAgencyRequest exige datos de identificación y contacto")
    void validaCreacionDeAgencia() {
        assertTrue(VALIDATOR.validate(new CreateAgencyRequest(
                "Rodadero Tours", "NIT-123", "Descripción", "Calle 1 # 2-3", "3001000001", null)).isEmpty());

        assertCampoInvalido(new CreateAgencyRequest(
                " ", "NIT-123", null, "Calle 1", "3001000001", null), "nombreComercial");
        assertCampoInvalido(new CreateAgencyRequest(
                "Rodadero Tours", " ", null, "Calle 1", "3001000001", null), "nitORegistro");
        assertCampoInvalido(new CreateAgencyRequest(
                "Rodadero Tours", "NIT-123", null, " ", "3001000001", null), "direccion");
        assertCampoInvalido(new CreateAgencyRequest(
                "Rodadero Tours", "NIT-123", null, "Calle 1", " ", null), "telefonoContacto");
    }

    @Test
    @DisplayName("CreateExperienceTypeRequest valida que el nombre no esté vacío ni sea demasiado largo")
    void validaTipoDeExperiencia() {
        assertTrue(VALIDATOR.validate(new CreateExperienceTypeRequest("Aventura", "Exploración")).isEmpty());
        assertCampoInvalido(new CreateExperienceTypeRequest(" ", null), "nombre");
        assertCampoInvalido(new CreateExperienceTypeRequest("x".repeat(81), null), "nombre");
    }

    @Test
    @DisplayName("CreateTourRequest valida relaciones, texto y valores numéricos")
    void validaCreacionDeTour() {
        CreateTourRequest valido = new CreateTourRequest(
                1L, 2L, "Tour Minca", "Descripción", "Minca",
                new BigDecimal("100000.00"), new BigDecimal("2.5"),
                DifficultyLevel.BAJO, "Guía", null);
        assertTrue(VALIDATOR.validate(valido).isEmpty());

        assertCampoInvalido(new CreateTourRequest(
                null, 2L, "Tour", "Descripción", "Minca",
                new BigDecimal("100000"), new BigDecimal("2.5"), DifficultyLevel.BAJO, null, null), "agenciaId");
        assertCampoInvalido(new CreateTourRequest(
                1L, null, "Tour", "Descripción", "Minca",
                new BigDecimal("100000"), new BigDecimal("2.5"), DifficultyLevel.BAJO, null, null), "tipoExperienciaId");
        assertCampoInvalido(new CreateTourRequest(
                1L, 2L, " ", "Descripción", "Minca",
                new BigDecimal("100000"), new BigDecimal("2.5"), DifficultyLevel.BAJO, null, null), "titulo");
        assertCampoInvalido(new CreateTourRequest(
                1L, 2L, "Tour", "Descripción", "Minca",
                new BigDecimal("0.00"), new BigDecimal("2.5"), DifficultyLevel.BAJO, null, null), "precio");
        assertCampoInvalido(new CreateTourRequest(
                1L, 2L, "Tour", "Descripción", "Minca",
                new BigDecimal("100000"), new BigDecimal("0.0"), DifficultyLevel.BAJO, null, null), "duracionHoras");
        assertCampoInvalido(new CreateTourRequest(
                1L, 2L, "Tour", "Descripción", "Minca",
                new BigDecimal("100000"), new BigDecimal("2.5"), null, null, null), "nivelDificultad");
    }

    @Test
    @DisplayName("CreateTourScheduleRequest admite guía opcional pero exige fecha, hora y cupos")
    void validaCreacionDeHorario() {
        LocalDate fecha = LocalDate.of(2026, 12, 10);
        LocalTime hora = LocalTime.of(9, 0);

        assertTrue(VALIDATOR.validate(new CreateTourScheduleRequest(null, fecha, hora, 10)).isEmpty());
        assertCampoInvalido(new CreateTourScheduleRequest(null, null, hora, 10), "fechaTour");
        assertCampoInvalido(new CreateTourScheduleRequest(null, fecha, null, 10), "horaInicio");
        assertCampoInvalido(new CreateTourScheduleRequest(null, fecha, hora, 0), "cuposTotales");
    }

    @Test
    @DisplayName("CreateReservationRequest exige turista, horario y al menos una persona")
    void validaCreacionDeReserva() {
        assertTrue(VALIDATOR.validate(new CreateReservationRequest(1L, 2L, 3, null)).isEmpty());
        assertCampoInvalido(new CreateReservationRequest(null, 2L, 3, null), "turistaId");
        assertCampoInvalido(new CreateReservationRequest(1L, null, 3, null), "horarioId");
        assertCampoInvalido(new CreateReservationRequest(1L, 2L, 0, null), "cantidadPersonas");
    }

    @Test
    @DisplayName("UpdateReservationRequest solo admite observaciones sin restricciones obligatorias")
    void validaActualizacionDeReserva() {
        assertTrue(VALIDATOR.validate(new UpdateReservationRequest(null)).isEmpty());
        assertTrue(VALIDATOR.validate(new UpdateReservationRequest("Nota de la reserva")).isEmpty());
    }

    @Test
    @DisplayName("CancelReservationRequest exige un motivo no vacío")
    void validaCancelacionDeReserva() {
        assertTrue(VALIDATOR.validate(new CancelReservationRequest("Cambio de planes")).isEmpty());
        assertCampoInvalido(new CancelReservationRequest(" "), "motivo");
    }

    @Test
    @DisplayName("CreateGuideRequest valida agencia, documento y correo")
    void validaCreacionDeGuia() {
        assertTrue(VALIDATOR.validate(new CreateGuideRequest(
                1L, "Guía Uno", "CC-123", "RG-123", "guia@example.com", "3002000001", "Español", null)).isEmpty());

        assertCampoInvalido(new CreateGuideRequest(
                null, "Guía Uno", "CC-123", "RG-123", "guia@example.com", "3002000001", null, null), "agenciaId");
        assertCampoInvalido(new CreateGuideRequest(
                1L, " ", "CC-123", "RG-123", "guia@example.com", "3002000001", null, null), "nombreCompleto");
        assertCampoInvalido(new CreateGuideRequest(
                1L, "Guía Uno", "CC-123", "RG-123", "correo-invalido", "3002000001", null, null), "correoElectronico");
    }

    @Test
    @DisplayName("La calificación de una reseña debe estar entre 1 y 5")
    void validaCalificacionDeResena() {
        assertTrue(VALIDATOR.validate(new CreateReviewRequest(1L, 5, "Excelente")).isEmpty());
        assertCampoInvalido(new CreateReviewRequest(1L, 0, null), "calificacion");
        assertCampoInvalido(new CreateReviewRequest(1L, 6, null), "calificacion");
        assertCampoInvalido(new UpdateReviewRequest(6, null), "calificacion");
    }

    @Test
    @DisplayName("Los mensajes de validación están en español y son coherentes con las entidades")
    void mensajesEnEspanol() {
        Set<String> mensajes = VALIDATOR.validate(new CreateReservationRequest(null, null, 0, null)).stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(mensajes.contains("El turista es obligatorio"));
        assertTrue(mensajes.contains("El horario de salida es obligatorio"));
        assertTrue(mensajes.contains("Debe reservarse al menos para 1 persona"));
    }

    private void assertCampoInvalido(Object dto, String campo) {
        boolean encontrado = VALIDATOR.validate(dto).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(campo));
        assertTrue(encontrado, "Se esperaba una violación en el campo " + campo);
    }
}
