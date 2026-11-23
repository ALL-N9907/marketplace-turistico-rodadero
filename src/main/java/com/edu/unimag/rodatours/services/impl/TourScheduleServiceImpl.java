package com.edu.unimag.rodatours.services.impl;

import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.api.dtos.request.CreateTourScheduleRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTourScheduleRequest;
import com.edu.unimag.rodatours.api.dtos.response.AvailabilitySlotResponse;
import com.edu.unimag.rodatours.api.dtos.response.TourScheduleResponse;
import com.edu.unimag.rodatours.domain.entities.Guide;
import com.edu.unimag.rodatours.domain.entities.Tour;
import com.edu.unimag.rodatours.domain.entities.TourSchedule;
import com.edu.unimag.rodatours.domain.repositories.GuideRepository;
import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import com.edu.unimag.rodatours.domain.repositories.TourScheduleRepository;
import com.edu.unimag.rodatours.services.TourScheduleService;
import com.edu.unimag.rodatours.services.exceptions.BusinessException;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.mapper.TourScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TourScheduleServiceImpl implements TourScheduleService {

    private static final List<ReservationStatus> ACTIVE_RESERVATION_STATES =
            List.of(ReservationStatus.SCHEDULED, ReservationStatus.CONFIRMED);

    private final TourScheduleRepository tourScheduleRepository;
    private final TourRepository tourRepository;
    private final GuideRepository guideRepository;
    private final ReservationRepository reservationRepository;

    @Override
    public TourScheduleResponse create(Long tourId, CreateTourScheduleRequest request) {
        Tour tour = findTourOrThrow(tourId);
        if (!tour.isActive()) {
            throw new BusinessException("No se pueden programar salidas para un tour inactivo.");
        }
        if (LocalDateTime.of(request.fechaTour(), request.horaInicio()).isBefore(LocalDateTime.now())) {
            throw new BusinessException("La fecha y hora de la salida no pueden estar en el pasado.");
        }
        if (tourScheduleRepository.existsByTourIdAndFechaTourAndHoraInicio(
                tourId, request.fechaTour(), request.horaInicio())) {
            throw new ConflictException("Ya existe una salida programada para este tour en la misma fecha y hora.");
        }

        Guide guide = resolveAndValidateGuide(request.guiaId(), tour, request.fechaTour(), request.horaInicio(), null);
        TourSchedule schedule = TourScheduleMapper.toEntity(request, tour, guide);
        TourSchedule saved = tourScheduleRepository.save(schedule);
        return TourScheduleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TourScheduleResponse getById(Long id) {
        TourSchedule schedule = findScheduleOrThrow(id);
        return TourScheduleMapper.toResponse(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourScheduleResponse> getByTourId(Long tourId, LocalDate fecha) {
        findTourOrThrow(tourId);
        List<TourSchedule> schedules = (fecha != null)
                ? tourScheduleRepository.findByTourIdAndFechaTourOrderByHoraInicioAsc(tourId, fecha)
                : tourScheduleRepository.findByTourIdOrderByFechaTourAscHoraInicioAsc(tourId);
        return schedules.stream()
                .map(TourScheduleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> getAvailableSlots(Long tourId, LocalDate fecha, int cantidadPersonas) {
        Tour tour = findTourOrThrow(tourId);
        if (!tour.isActive() || !tour.getAgencia().isActive()) {
            return List.of();
        }
        if (fecha == null) {
            throw new BusinessException("La fecha de consulta es obligatoria.");
        }
        if (cantidadPersonas < 1) {
            throw new BusinessException("La cantidad de personas debe ser al menos 1.");
        }
        return tourScheduleRepository.findWithAvailableSpots(tourId, fecha, cantidadPersonas).stream()
                .filter(schedule -> schedule.getStartDateTime().isAfter(LocalDateTime.now()))
                .map(schedule -> {
                    Long held = reservationRepository.sumPersonasByHorarioIdAndEstado(schedule.getId(), ReservationStatus.SCHEDULED);
                    int available = Math.max(0, schedule.getCuposDisponibles() - (held == null ? 0 : Math.toIntExact(held)));
                    return new AvailabilitySlotResponse(schedule.getId(), tourId, schedule.getFechaTour(),
                            schedule.getHoraInicio(), schedule.calculateEndDateTime(), schedule.getCuposTotales(), available);
                })
                .filter(slot -> slot.cuposDisponibles() >= cantidadPersonas)
                .toList();
    }

    @Override
    public TourScheduleResponse assignGuide(Long scheduleId, Long guiaId) {
        TourSchedule schedule = findScheduleOrThrow(scheduleId);
        Guide guide = resolveAndValidateGuide(
                guiaId, schedule.getTour(), schedule.getFechaTour(), schedule.getHoraInicio(), schedule.getId());
        schedule.setGuia(guide);
        TourSchedule updated = tourScheduleRepository.save(schedule);
        return TourScheduleMapper.toResponse(updated);
    }

    @Override
    public TourScheduleResponse update(Long id, UpdateTourScheduleRequest request) {
        TourSchedule schedule = findScheduleOrThrow(id);
        Tour tour = schedule.getTour();

        boolean dateTimeChanged = !schedule.getFechaTour().equals(request.fechaTour())
                || !schedule.getHoraInicio().equals(request.horaInicio());
        if (dateTimeChanged) {
            if (!tour.isActive()) {
                throw new BusinessException("No se puede reprogramar una salida de un tour inactivo.");
            }
            if (LocalDateTime.of(request.fechaTour(), request.horaInicio()).isBefore(LocalDateTime.now())) {
                throw new BusinessException("La nueva fecha y hora de la salida no pueden estar en el pasado.");
            }
            // fechaInicio/fechaFin de cada reserva se calculan al crearla; reprogramar las dejaría desfasadas.
            if (reservationRepository.existsByHorarioIdAndEstadoIn(schedule.getId(), ACTIVE_RESERVATION_STATES)) {
                throw new ConflictException("No se puede cambiar la fecha u hora de una salida con reservas activas "
                        + "(SCHEDULED o CONFIRMED). Cancele esas reservas primero.");
            }
        }
        if (dateTimeChanged && tourScheduleRepository.existsByTourIdAndFechaTourAndHoraInicio(
                tour.getId(), request.fechaTour(), request.horaInicio())) {
            throw new ConflictException("Ya existe otra salida programada para este tour en la misma fecha y hora.");
        }

        int cuposOcupados = schedule.getCuposTotales() - schedule.getCuposDisponibles();
        Long pendientesRetenidos = reservationRepository
                .sumPersonasByHorarioIdAndEstado(schedule.getId(), ReservationStatus.SCHEDULED);
        int pendientes = pendientesRetenidos == null ? 0 : pendientesRetenidos.intValue();
        if (request.cuposTotales() < cuposOcupados + pendientes) {
            throw new BusinessException("El nuevo total de cupos (" + request.cuposTotales()
                    + ") no puede ser menor que los cupos ocupados (" + cuposOcupados
                    + ") más los retenidos por reservas pendientes (" + pendientes + ").");
        }

        Guide guide = resolveAndValidateGuide(
                request.guiaId(), tour, request.fechaTour(), request.horaInicio(), schedule.getId());

        TourScheduleMapper.applyUpdate(request, schedule, guide);
        schedule.setCuposDisponibles(request.cuposTotales() - cuposOcupados);

        TourSchedule updated = tourScheduleRepository.save(schedule);
        return TourScheduleMapper.toResponse(updated);
    }

    @Override
    public void delete(Long id) {
        TourSchedule schedule = findScheduleOrThrow(id);
        if (!schedule.getReservas().isEmpty()) {
            throw new ConflictException("No se puede eliminar una salida que tiene reservas asociadas.");
        }
        tourScheduleRepository.delete(schedule);
    }

    private Guide resolveAndValidateGuide(Long guiaId, Tour tour, LocalDate fechaTour,
                                          LocalTime horaInicio, Long excludeScheduleId) {
        if (guiaId == null) {
            return null;
        }
        Guide guide = guideRepository.findById(guiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Guía no encontrado con id: " + guiaId));

        if (!guide.isActive()) {
            throw new BusinessException("El guía seleccionado se encuentra inactivo.");
        }
        if (!guide.getAgencia().getId().equals(tour.getAgencia().getId())) {
            throw new BusinessException("El guía debe pertenecer a la misma agencia que ofrece el tour.");
        }

        LocalDateTime start = LocalDateTime.of(fechaTour, horaInicio);
        long minutes = (long) (tour.getDuracionHoras().doubleValue() * 60);
        LocalDateTime end = start.plusMinutes(minutes);

        boolean hasOverlap = tourScheduleRepository.findByGuiaIdAndFechaTourInOrderByHoraInicioAsc(
                        guiaId, List.of(start.toLocalDate(), end.toLocalDate())).stream()
                .filter(existing -> excludeScheduleId == null || !existing.getId().equals(excludeScheduleId))
                .anyMatch(existing -> {
                    LocalDateTime sStart = existing.getStartDateTime();
                    LocalDateTime sEnd = existing.calculateEndDateTime();
                    return sStart != null && sEnd != null && sStart.isBefore(end) && sEnd.isAfter(start);
                });

        if (hasOverlap) {
            throw new ConflictException("El guía ya tiene otra salida asignada que se traslapa en ese horario.");
        }
        return guide;
    }

    private TourSchedule findScheduleOrThrow(Long id) {
        return tourScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horario de salida no encontrado con id: " + id));
    }

    private Tour findTourOrThrow(Long id) {
        return tourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour no encontrado con id: " + id));
    }
}
