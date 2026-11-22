package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.api.dtos.request.CreateTouristRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTouristRequest;
import com.edu.unimag.rodatours.api.dtos.response.TouristResponse;
import com.edu.unimag.rodatours.domain.entities.enums.TouristStatus;

import java.util.List;

public interface TouristService {

    TouristResponse create(CreateTouristRequest request);

    TouristResponse getById(Long id);

    TouristResponse getByEmail(String correoElectronico);

    List<TouristResponse> getAll();

    List<TouristResponse> getByStatus(TouristStatus estado);

    TouristResponse update(Long id, UpdateTouristRequest request);

    void delete(Long id);
}
