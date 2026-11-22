package com.edu.unimag.rodatours.services.impl;

import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import com.edu.unimag.rodatours.api.dtos.request.CreateExperienceTypeRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateExperienceTypeRequest;
import com.edu.unimag.rodatours.api.dtos.response.ExperienceTypeResponse;
import com.edu.unimag.rodatours.domain.entities.ExperienceType;
import com.edu.unimag.rodatours.domain.repositories.ExperienceTypeRepository;
import com.edu.unimag.rodatours.services.ExperienceTypeService;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.mapper.ExperienceTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExperienceTypeServiceImpl implements ExperienceTypeService {

    private final ExperienceTypeRepository experienceTypeRepository;
    private final TourRepository tourRepository;

    @Override
    public ExperienceTypeResponse create(CreateExperienceTypeRequest request) {
        if (experienceTypeRepository.existsByNombreIgnoreCase(request.nombre().trim())) {
            throw new ConflictException("Ya existe un tipo de experiencia con el nombre: " + request.nombre());
        }
        ExperienceType entity = ExperienceTypeMapper.toEntity(request);
        ExperienceType saved = experienceTypeRepository.save(entity);
        return ExperienceTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExperienceTypeResponse getById(Long id) {
        ExperienceType entity = findExperienceTypeOrThrow(id);
        return ExperienceTypeMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExperienceTypeResponse> getAll() {
        return experienceTypeRepository.findAllByOrderByNombreAsc().stream()
                .map(ExperienceTypeMapper::toResponse)
                .toList();
    }

    @Override
    public ExperienceTypeResponse update(Long id, UpdateExperienceTypeRequest request) {
        ExperienceType entity = findExperienceTypeOrThrow(id);
        if (!entity.getNombre().equalsIgnoreCase(request.nombre().trim())
                && experienceTypeRepository.existsByNombreIgnoreCase(request.nombre().trim())) {
            throw new ConflictException("Ya existe otro tipo de experiencia con el nombre: " + request.nombre());
        }
        ExperienceTypeMapper.applyUpdate(request, entity);
        ExperienceType updated = experienceTypeRepository.save(entity);
        return ExperienceTypeMapper.toResponse(updated);
    }

    @Override
    public void delete(Long id) {
        ExperienceType entity = findExperienceTypeOrThrow(id);
        if (tourRepository.existsByTipoExperienciaId(id)) {
            throw new ConflictException("No se puede eliminar un tipo de experiencia que tiene tours asociados.");
        }
        experienceTypeRepository.delete(entity);
    }

    private ExperienceType findExperienceTypeOrThrow(Long id) {
        return experienceTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de experiencia no encontrado con id: " + id));
    }
}
