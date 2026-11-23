package com.edu.unimag.rodatours.services.impl;

import com.edu.unimag.rodatours.api.dtos.request.CancelReservationRequest;
import com.edu.unimag.rodatours.api.dtos.request.CreateReservationRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateReservationRequest;
import com.edu.unimag.rodatours.api.dtos.response.AgencyOccupancyResponse;
import com.edu.unimag.rodatours.api.dtos.response.AgencyProductivityResponse;
import com.edu.unimag.rodatours.api.dtos.response.NoShowTouristResponse;
import com.edu.unimag.rodatours.api.dtos.response.ReservationResponse;
import com.edu.unimag.rodatours.domain.entities.Reservation;
import com.edu.unimag.rodatours.domain.entities.Tour;
import com.edu.unimag.rodatours.domain.entities.TourSchedule;
import com.edu.unimag.rodatours.domain.entities.Tourist;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.domain.repositories.TourScheduleRepository;
import com.edu.unimag.rodatours.domain.repositories.TouristRepository;
import com.edu.unimag.rodatours.domain.repositories.projections.AgencyOccupancyProjection;
import com.edu.unimag.rodatours.domain.repositories.projections.AgencyProductivityProjection;
import com.edu.unimag.rodatours.services.ReservationService;
import com.edu.unimag.rodatours.services.exceptions.BusinessException;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.mapper.ReportMapper;
import com.edu.unimag.rodatours.services.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import com.edu.unimag.rodatours.notifications.ReservationChanged;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private static final List<ReservationStatus> ACTIVE_RESERVATION_STATES =
            List.of(ReservationStatus.SCHEDULED, ReservationStatus.CONFIRMED);

    private static final List<ReservationStatus> OCCUPIED_SPOT_STATES =
            // NO_SHOW sigue ocupando el cupo: se descontó al confirmar y nunca se libera.
            List.of(ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED, ReservationStatus.NO_SHOW);

    private final ReservationRepository reservationRepository;
    private final TouristRepository touristRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final ApplicationEventPublisher events;

    @Override
    public ReservationResponse create(CreateReservationRequest request) {
        // Serializa reservas del mismo turista incluso si apuntan a salidas distintas.
        Tourist tourist = touristRepository.findByIdForUpdate(request.turistaId())
                .orElseThrow(() -> new ResourceNotFoundException("Turista no encontrado con id: " + request.turistaId()));
        if (!tourist.isActive()) {
            throw new BusinessException("El turista se encuentra inactivo y no puede realizar reservas.");
        }

        // Bloqueo pesimista: serializa las creaciones concurrentes sobre la misma salida.
        TourSchedule schedule = tourScheduleRepository.findByIdForUpdate(request.horarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Horario de salida no encontrado con id: " + request.horarioId()));

        Tour tour = schedule.getTour();
        if (tour == null || !tour.isActive() || (tour.getAgencia() != null && !tour.getAgencia().isActive())) {
            throw new BusinessException("El tour o la agencia asociada no se encuentran activos.");
        }

        LocalDateTime fechaInicio = schedule.getStartDateTime();
        LocalDateTime fechaFin = schedule.calculateEndDateTime();
        if (fechaInicio == null || fechaFin == null) {
            throw new BusinessException("No se pudo determinar el rango horario de la salida.");
        }
        if (fechaInicio.isBefore(LocalDateTime.now())) {
            throw new BusinessException("No se pueden crear reservas para salidas en el pasado.");
        }

        // Las reservas SCHEDULED retienen cupo aunque aún no se hayan descontado (eso ocurre al confirmar).
        Long pendientesRetenidos = reservationRepository
                .sumPersonasByHorarioIdAndEstado(schedule.getId(), ReservationStatus.SCHEDULED);
        int pendientes = pendientesRetenidos == null ? 0 : pendientesRetenidos.intValue();
        if (!schedule.hasAvailability(request.cantidadPersonas() + pendientes)) {
            throw new ConflictException("No hay suficientes cupos disponibles para la salida seleccionada "
                    + "(se consideran las reservas pendientes de confirmación).");
        }

        if (reservationRepository.existsOverlappingByTuristaAndStates(
                tourist.getId(), ACTIVE_RESERVATION_STATES, fechaInicio, fechaFin)) {
            throw new ConflictException("El turista ya tiene una reserva activa que se traslapa con este horario.");
        }

        BigDecimal precioTotal = tour.getPrecio().multiply(BigDecimal.valueOf(request.cantidadPersonas()));
        String codigoQr = UUID.randomUUID().toString();

        Reservation reservation = ReservationMapper.toEntity(
                request, tourist, schedule, fechaInicio, fechaFin, precioTotal, codigoQr);
        Reservation saved = reservationRepository.save(reservation);
        events.publishEvent(ReservationChanged.from(saved));
        return ReservationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id) {
        return ReservationMapper.toResponse(findReservationOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getByQrCode(String codigoQr) {
        Reservation reservation = reservationRepository.findByCodigoQr(codigoQr)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada para el código QR suministrado."));
        return ReservationMapper.toResponse(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getAll() {
        return reservationRepository.findAll().stream()
                .map(ReservationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getByTourist(Long turistaId, ReservationStatus estado) {
        if (!touristRepository.existsById(turistaId)) {
            throw new ResourceNotFoundException("Turista no encontrado con id: " + turistaId);
        }
        List<Reservation> reservations = (estado != null)
                ? reservationRepository.findByTuristaIdAndEstado(turistaId, estado)
                : reservationRepository.findByTuristaIdOrderByFechaReservaDesc(turistaId);
        return reservations.stream()
                .map(ReservationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getBySchedule(Long horarioId) {
        if (!tourScheduleRepository.existsById(horarioId)) {
            throw new ResourceNotFoundException("Horario de salida no encontrado con id: " + horarioId);
        }
        return reservationRepository.findByHorarioId(horarioId).stream()
                .map(ReservationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getByReservationDateRange(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new BusinessException("El rango de fechas de consulta es inválido.");
        }
        return reservationRepository.findByFechaReservaBetween(desde, hasta).stream()
                .map(ReservationMapper::toResponse)
                .toList();
    }

    @Override
    public ReservationResponse updateNotes(Long id, UpdateReservationRequest request) {
        Reservation reservation = findReservationOrThrow(id);
        ReservationMapper.applyUpdate(request, reservation);
        return ReservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Override
    public ReservationResponse confirm(Long id) {
        Reservation reservation = findReservationOrThrow(id);
        if (!reservation.getEstado().canTransitionTo(ReservationStatus.CONFIRMED)) {
            throw new ConflictException("La reserva no puede confirmarse desde el estado actual: " + reservation.getEstado());
        }

        TourSchedule lockedSchedule = tourScheduleRepository.findByIdForUpdate(reservation.getHorario().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Horario de salida no encontrado."));

        if (!lockedSchedule.hasAvailability(reservation.getCantidadPersonas())) {
            throw new ConflictException("No hay cupos disponibles suficientes para confirmar la reserva.");
        }

        lockedSchedule.discountSpots(reservation.getCantidadPersonas());
        reservation.confirm();

        tourScheduleRepository.save(lockedSchedule);
        Reservation saved = reservationRepository.save(reservation);
        events.publishEvent(ReservationChanged.from(saved));
        return ReservationMapper.toResponse(saved);
    }

    @Override
    public ReservationResponse cancel(Long id, CancelReservationRequest request) {
        Reservation reservation = findReservationOrThrow(id);
        ReservationStatus previousStatus = reservation.getEstado();
        if (!previousStatus.canTransitionTo(ReservationStatus.CANCELLED)) {
            throw new ConflictException("La reserva no puede cancelarse desde el estado actual: " + previousStatus);
        }

        String motivo = request != null ? request.motivo() : null;
        if (motivo == null || motivo.isBlank()) {
            throw new BusinessException("El motivo de cancelación es obligatorio.");
        }
        TourSchedule horario = reservation.getHorario();
        LocalDateTime inicioSalida = horario == null ? null : horario.getStartDateTime();
        if (inicioSalida != null && !inicioSalida.isAfter(LocalDateTime.now())) {
            throw new BusinessException("No se puede cancelar una reserva de una salida que ya inició.");
        }

        if (previousStatus == ReservationStatus.CONFIRMED) {
            TourSchedule lockedSchedule = tourScheduleRepository.findByIdForUpdate(reservation.getHorario().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Horario de salida no encontrado."));
            lockedSchedule.releaseSpots(reservation.getCantidadPersonas());
            tourScheduleRepository.save(lockedSchedule);
        }

        try {
            reservation.cancel(motivo);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ex.getMessage());
        }

        Reservation saved = reservationRepository.save(reservation);
        events.publishEvent(ReservationChanged.from(saved));
        return ReservationMapper.toResponse(saved);
    }

    @Override
    public ReservationResponse complete(Long id, String observacionesAdicionales) {
        Reservation reservation = findReservationOrThrow(id);
        if (!reservation.getEstado().canTransitionTo(ReservationStatus.COMPLETED)) {
            throw new ConflictException("Solo reservas confirmadas pueden marcarse como completadas. Estado actual: " + reservation.getEstado());
        }
        LocalDate fechaTour = reservation.getHorario().getFechaTour();
        if (fechaTour != null && fechaTour.isAfter(LocalDate.now())) {
            throw new BusinessException("No se puede completar una reserva cuya fecha de tour es posterior a hoy.");
        }

        reservation.complete(observacionesAdicionales);
        events.publishEvent(ReservationChanged.from(reservation));
        return ReservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Override
    public ReservationResponse markNoShow(Long id) {
        Reservation reservation = findReservationOrThrow(id);
        if (!reservation.getEstado().canTransitionTo(ReservationStatus.NO_SHOW)) {
            throw new ConflictException("Solo reservas confirmadas pueden marcarse como NO_SHOW. Estado actual: " + reservation.getEstado());
        }
        LocalDate fechaTour = reservation.getHorario().getFechaTour();
        if (fechaTour != null && fechaTour.isAfter(LocalDate.now())) {
            throw new BusinessException("No se puede marcar NO_SHOW antes de la fecha programada del tour.");
        }

        reservation.markNoShow();
        events.publishEvent(ReservationChanged.from(reservation));
        return ReservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Override
    @Transactional(readOnly = true)
    public AgencyOccupancyResponse getAgencyOccupancy(Long agenciaId, LocalDate desde, LocalDate hasta) {
        validateDateRange(desde, hasta);
        AgencyOccupancyProjection projection = reservationRepository
                .findAgencyOccupancy(agenciaId, desde, hasta, OCCUPIED_SPOT_STATES)
                .orElseThrow(() -> new ResourceNotFoundException("Agencia no encontrada con id: " + agenciaId));
        return ReportMapper.toOccupancyResponse(projection, desde, hasta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgencyProductivityResponse> getAgencyProductivityRanking(LocalDate desde, LocalDate hasta) {
        validateDateRange(desde, hasta);
        List<AgencyProductivityProjection> ranking = reservationRepository
                .rankingAgenciesByCompletedTours(ReservationStatus.COMPLETED, desde, hasta);
        List<AgencyProductivityResponse> responses = new ArrayList<>(ranking.size());
        for (int i = 0; i < ranking.size(); i++) {
            responses.add(ReportMapper.toProductivityResponse(ranking.get(i), i + 1));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoShowTouristResponse> getNoShowTouristsReport(LocalDate desde, LocalDate hasta) {
        validateDateRange(desde, hasta);
        return reservationRepository.findNoShowTourists(ReservationStatus.NO_SHOW, desde, hasta).stream()
                .map(projection -> ReportMapper.toNoShowResponse(projection, desde, hasta))
                .toList();
    }

    private void validateDateRange(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new BusinessException("El rango de fechas suministrado es inválido.");
        }
    }

    private Reservation findReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con id: " + id));
    }
}
