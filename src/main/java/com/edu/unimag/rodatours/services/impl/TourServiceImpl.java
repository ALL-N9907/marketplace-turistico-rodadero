package com.edu.unimag.rodatours.services.impl;

import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.TourScheduleRepository;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.api.dtos.request.CreateTourRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTourRequest;
import com.edu.unimag.rodatours.api.dtos.response.TourResponse;
import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.ExperienceType;
import com.edu.unimag.rodatours.domain.entities.Tour;
import com.edu.unimag.rodatours.domain.entities.enums.DifficultyLevel;
import com.edu.unimag.rodatours.domain.entities.enums.TourStatus;
import com.edu.unimag.rodatours.domain.repositories.AgencyRepository;
import com.edu.unimag.rodatours.domain.repositories.ExperienceTypeRepository;
import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import com.edu.unimag.rodatours.services.TourService;
import com.edu.unimag.rodatours.services.exceptions.BusinessException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.mapper.TourMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TourServiceImpl implements TourService {

    private static final List<ReservationStatus> ACTIVE_RESERVATION_STATES =
            List.of(ReservationStatus.SCHEDULED, ReservationStatus.CONFIRMED);

    private final TourRepository tourRepository;
    private final AgencyRepository agencyRepository;
    private final ExperienceTypeRepository experienceTypeRepository;
    private final ReservationRepository reservationRepository;
    private final TourScheduleRepository tourScheduleRepository;

    @Override
    public TourResponse create(CreateTourRequest request) {
        Agency agency = findAgencyOrThrow(request.agenciaId());
        if (!agency.isActive()) {
            throw new BusinessException("No se pueden crear tours para una agencia inactiva.");
        }
        ExperienceType experienceType = findExperienceTypeOrThrow(request.tipoExperienciaId());

        Tour tour = TourMapper.toEntity(request, agency, experienceType);
        Tour saved = tourRepository.save(tour);
        return TourMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TourResponse getById(Long id) {
        Tour tour = findTourOrThrow(id);
        return TourMapper.toResponse(tour);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourResponse> getAll() {
        return tourRepository.findAll().stream()
                .map(TourMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourResponse> getByAgency(Long agenciaId, DifficultyLevel dificultad, String destino) {
        findAgencyOrThrow(agenciaId);
        List<Tour> tours;
        boolean hasDestino = destino != null && !destino.isBlank();
        if (dificultad != null && hasDestino) {
            tours = tourRepository.findByAgenciaIdAndNivelDificultadAndDestinoContainingIgnoreCase(
                    agenciaId, dificultad, destino.trim());
        } else if (dificultad != null) {
            tours = tourRepository.findByAgenciaIdAndNivelDificultad(agenciaId, dificultad);
        } else if (hasDestino) {
            tours = tourRepository.findByAgenciaIdAndDestinoContainingIgnoreCase(agenciaId, destino.trim());
        } else {
            tours = tourRepository.findByAgenciaId(agenciaId);
        }
        return tours.stream()
                .map(TourMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourResponse> findAvailableByDestinationAndDate(String destino, LocalDate fecha, int cantidadPersonas) {
        if (fecha == null) {
            throw new BusinessException("La fecha de consulta es obligatoria.");
        }
        if (cantidadPersonas < 1) {
            throw new BusinessException("La cantidad de personas debe ser al menos 1.");
        }
        String normalizedDestino = (destino == null || destino.isBlank()) ? null : destino.trim();
        return tourRepository.findAvailableByDestinationAndDate(
                        TourStatus.ACTIVE, normalizedDestino, fecha, cantidadPersonas).stream()
                .map(TourMapper::toResponse)
                .toList();
    }

    @Override
    public TourResponse update(Long id, UpdateTourRequest request) {
        Tour tour = findTourOrThrow(id);
        Agency agency = findAgencyOrThrow(request.agenciaId());
        ExperienceType experienceType = findExperienceTypeOrThrow(request.tipoExperienciaId());

        boolean agencyChanged = tour.getAgencia() == null || !agency.getId().equals(tour.getAgencia().getId());
        if (agencyChanged) {
            if (!agency.isActive()) {
                throw new BusinessException("No se puede asignar el tour a una agencia inactiva.");
            }
            if (reservationRepository.existsByHorarioTourId(id)) {
                throw new ConflictException("No se puede cambiar la agencia de un tour con reservas asociadas.");
            }
            if (tourScheduleRepository.existsByTourIdAndGuiaIsNotNull(id)) {
                throw new ConflictException("No se puede cambiar la agencia de un tour con salidas que tienen guía "
                        + "asignado. Retire o reasigne los guías primero.");
            }
        }

        // El fin de cada reserva se calcula con la duración del tour al crearla.
        boolean durationChanged = tour.getDuracionHoras() == null
                || request.duracionHoras().compareTo(tour.getDuracionHoras()) != 0;
        if (durationChanged && reservationRepository.existsByHorarioTourIdAndEstadoIn(id, ACTIVE_RESERVATION_STATES)) {
            throw new ConflictException("No se puede cambiar la duración de un tour con reservas activas "
                    + "(SCHEDULED o CONFIRMED).");
        }

        TourMapper.applyUpdate(request, tour, agency, experienceType);
        Tour updated = tourRepository.save(tour);
        return TourMapper.toResponse(updated);
    }

    @Override
    public void delete(Long id) {
        Tour tour = findTourOrThrow(id);
        if (reservationRepository.existsByHorarioTourId(id)) {
            throw new ConflictException("No se puede eliminar un tour con reservas asociadas. "
                    + "Desactívelo cambiando su estado a INACTIVE.");
        }
        tourRepository.delete(tour);
    }

    private Tour findTourOrThrow(Long id) {
        return tourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour no encontrado con id: " + id));
    }

    private Agency findAgencyOrThrow(Long id) {
        return agencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agencia no encontrada con id: " + id));
    }

    private ExperienceType findExperienceTypeOrThrow(Long id) {
        return experienceTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de experiencia no encontrado con id: " + id));
    }
}
