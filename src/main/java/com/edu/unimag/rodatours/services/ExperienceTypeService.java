package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.api.dtos.request.CreateExperienceTypeRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateExperienceTypeRequest;
import com.edu.unimag.rodatours.api.dtos.response.ExperienceTypeResponse;

import java.util.List;

public interface ExperienceTypeService {

    ExperienceTypeResponse create(CreateExperienceTypeRequest request);

    ExperienceTypeResponse getById(Long id);

    List<ExperienceTypeResponse> getAll();

    ExperienceTypeResponse update(Long id, UpdateExperienceTypeRequest request);

    void delete(Long id);
}
