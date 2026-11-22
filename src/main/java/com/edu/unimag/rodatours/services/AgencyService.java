package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.api.dtos.request.CreateAgencyRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateAgencyRequest;
import com.edu.unimag.rodatours.api.dtos.response.AgencyResponse;

import java.util.List;

public interface AgencyService {

    AgencyResponse create(CreateAgencyRequest request);

    AgencyResponse getById(Long id);

    List<AgencyResponse> getAll();

    List<AgencyResponse> getAllActive();

    List<AgencyResponse> searchActiveByNameOrLocation(String query);

    AgencyResponse update(Long id, UpdateAgencyRequest request);

    void delete(Long id);
}
