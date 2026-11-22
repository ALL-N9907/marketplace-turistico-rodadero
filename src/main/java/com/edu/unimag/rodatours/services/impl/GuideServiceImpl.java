package com.edu.unimag.rodatours.services.impl;

import com.edu.unimag.rodatours.api.dtos.request.CreateGuideRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateGuideRequest;
import com.edu.unimag.rodatours.api.dtos.response.GuideResponse;
import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.Guide;
import com.edu.unimag.rodatours.domain.entities.enums.GuideStatus;
import com.edu.unimag.rodatours.domain.repositories.AgencyRepository;
import com.edu.unimag.rodatours.domain.repositories.GuideRepository;
import com.edu.unimag.rodatours.services.GuideService;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.mapper.GuideMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GuideServiceImpl implements GuideService {

    private final GuideRepository guideRepository;
    private final AgencyRepository agencyRepository;

    @Override
    public GuideResponse create(CreateGuideRequest request) {
        Agency agency = findAgencyOrThrow(request.agenciaId());
        validateUniqueFields(request.documentoIdentidad(), request.registroGuia(), request.correoElectronico(), null);

        Guide guide = GuideMapper.toEntity(request, agency);
        Guide saved = guideRepository.save(guide);
        return GuideMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GuideResponse getById(Long id) {
        Guide guide = findGuideOrThrow(id);
        return GuideMapper.toResponse(guide);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuideResponse> getAll() {
        return guideRepository.findAll().stream()
                .map(GuideMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuideResponse> getByAgencyAndStatus(Long agenciaId, GuideStatus estado) {
        findAgencyOrThrow(agenciaId);
        GuideStatus filterStatus = estado == null ? GuideStatus.ACTIVE : estado;
        return guideRepository.findByAgenciaIdAndEstado(agenciaId, filterStatus).stream()
                .map(GuideMapper::toResponse)
                .toList();
    }

    @Override
    public GuideResponse update(Long id, UpdateGuideRequest request) {
        Guide guide = findGuideOrThrow(id);
        Agency agency = findAgencyOrThrow(request.agenciaId());
        validateUniqueFields(request.documentoIdentidad(), request.registroGuia(), request.correoElectronico(), guide);

        GuideMapper.applyUpdate(request, guide, agency);
        Guide updated = guideRepository.save(guide);
        return GuideMapper.toResponse(updated);
    }

    @Override
    public void delete(Long id) {
        Guide guide = findGuideOrThrow(id);
        guideRepository.delete(guide);
    }

    private void validateUniqueFields(String documento, String registro, String correo, Guide current) {
        if ((current == null || !current.getDocumentoIdentidad().equals(documento))
                && guideRepository.existsByDocumentoIdentidad(documento)) {
            throw new ConflictException("Ya existe un guía con el documento de identidad: " + documento);
        }
        if ((current == null || !current.getRegistroGuia().equals(registro))
                && guideRepository.existsByRegistroGuia(registro)) {
            throw new ConflictException("Ya existe un guía con el registro profesional: " + registro);
        }
        if ((current == null || !current.getCorreoElectronico().equalsIgnoreCase(correo))
                && guideRepository.existsByCorreoElectronicoIgnoreCase(correo)) {
            throw new ConflictException("Ya existe un guía con el correo electrónico: " + correo);
        }
    }

    private Guide findGuideOrThrow(Long id) {
        return guideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guía no encontrado con id: " + id));
    }

    private Agency findAgencyOrThrow(Long id) {
        return agencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agencia no encontrada con id: " + id));
    }
}
