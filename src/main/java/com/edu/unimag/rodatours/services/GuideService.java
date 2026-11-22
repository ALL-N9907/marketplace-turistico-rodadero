package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.api.dtos.request.CreateGuideRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateGuideRequest;
import com.edu.unimag.rodatours.api.dtos.response.GuideResponse;
import com.edu.unimag.rodatours.domain.entities.enums.GuideStatus;

import java.util.List;

public interface GuideService {

    GuideResponse create(CreateGuideRequest request);

    GuideResponse getById(Long id);

    List<GuideResponse> getAll();

    List<GuideResponse> getByAgencyAndStatus(Long agenciaId, GuideStatus estado);

    GuideResponse update(Long id, UpdateGuideRequest request);

    void delete(Long id);
}
