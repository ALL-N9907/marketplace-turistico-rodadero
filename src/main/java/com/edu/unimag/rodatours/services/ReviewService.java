package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.api.dtos.request.CreateReviewRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateReviewRequest;
import com.edu.unimag.rodatours.api.dtos.response.ReviewResponse;

import java.util.List;

public interface ReviewService {

    ReviewResponse create(Long turistaId, CreateReviewRequest request);

    ReviewResponse getById(Long id);

    ReviewResponse getByReservationId(Long reservaId);

    List<ReviewResponse> getByTouristId(Long turistaId);

    ReviewResponse update(Long id, Long turistaId, UpdateReviewRequest request);

    void delete(Long id, Long turistaId);
}
