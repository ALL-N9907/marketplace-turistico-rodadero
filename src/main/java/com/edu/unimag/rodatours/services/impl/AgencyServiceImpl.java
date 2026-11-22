package com.edu.unimag.rodatours.services.impl;

import com.edu.unimag.rodatours.domain.repositories.GuideRepository;
import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import com.edu.unimag.rodatours.api.dtos.request.CreateAgencyRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateAgencyRequest;
import com.edu.unimag.rodatours.api.dtos.response.AgencyResponse;
import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.enums.AgencyStatus;
import com.edu.unimag.rodatours.domain.repositories.AgencyRepository;
import com.edu.unimag.rodatours.services.AgencyService;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.mapper.AgencyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AgencyServiceImpl implements AgencyService {

    private final AgencyRepository agencyRepository;
    private final TourRepository tourRepository;
    private final GuideRepository guideRepository;

    @Override
    public AgencyResponse create(CreateAgencyRequest request) {
        if (agencyRepository.existsByNitORegistro(request.nitORegistro())) {
            throw new ConflictException("Ya existe una agencia registrada con el NIT o registro: " + request.nitORegistro());
        }
        Agency agency = AgencyMapper.toEntity(request);
        Agency saved = agencyRepository.save(agency);
        return AgencyMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AgencyResponse getById(Long id) {
        Agency agency = findAgencyOrThrow(id);
        return AgencyMapper.toResponse(agency);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgencyResponse> getAll() {
        return agencyRepository.findAll().stream()
                .map(AgencyMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgencyResponse> getAllActive() {
        return agencyRepository.findByEstadoOrderByNombreComercialAsc(AgencyStatus.ACTIVE).stream()
                .map(AgencyMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgencyResponse> searchActiveByNameOrLocation(String query) {
        if (query == null || query.isBlank()) {
            return getAllActive();
        }
        return agencyRepository.searchActiveByNameOrLocation(AgencyStatus.ACTIVE, query.trim()).stream()
                .map(AgencyMapper::toResponse)
                .toList();
    }

    @Override
    public AgencyResponse update(Long id, UpdateAgencyRequest request) {
        Agency agency = findAgencyOrThrow(id);
        if (!agency.getNitORegistro().equals(request.nitORegistro())
                && agencyRepository.existsByNitORegistro(request.nitORegistro())) {
            throw new ConflictException("Ya existe otra agencia con el NIT o registro: " + request.nitORegistro());
        }
        AgencyMapper.applyUpdate(request, agency);
        Agency updated = agencyRepository.save(agency);
        return AgencyMapper.toResponse(updated);
    }

    @Override
    public void delete(Long id) {
        Agency agency = findAgencyOrThrow(id);
        if (tourRepository.existsByAgenciaId(id)) {
            throw new ConflictException("No se puede eliminar una agencia con tours asociados. "
                    + "Desactívela cambiando su estado a INACTIVE.");
        }
        if (guideRepository.existsByAgenciaId(id)) {
            throw new ConflictException("No se puede eliminar una agencia con guías asociados. "
                    + "Desactívela cambiando su estado a INACTIVE.");
        }
        agencyRepository.delete(agency);
    }

    private Agency findAgencyOrThrow(Long id) {
        return agencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agencia no encontrada con id: " + id));
    }
}
