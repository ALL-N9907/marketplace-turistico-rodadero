package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.api.dtos.response.TourResponse;
import com.edu.unimag.rodatours.domain.entities.Tour;
import com.edu.unimag.rodatours.domain.entities.enums.AgencyStatus;
import com.edu.unimag.rodatours.domain.entities.enums.DifficultyLevel;
import com.edu.unimag.rodatours.domain.entities.enums.TourStatus;
import com.edu.unimag.rodatours.domain.repositories.AgencyRepository;
import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import com.edu.unimag.rodatours.services.mapper.TourMapper;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final TourRepository tours;
    private final AgencyRepository agencies;

    @Transactional(readOnly = true)
    public SearchResponse search(Filter filter, int page, int size) {
        if (filter.minPrice() != null && filter.maxPrice() != null && filter.minPrice().compareTo(filter.maxPrice()) > 0
                || filter.minHours() != null && filter.maxHours() != null && filter.minHours().compareTo(filter.maxHours()) > 0) {
            throw new ValidationException("Los rangos de precio y duración deben estar en orden ascendente.");
        }
        Specification<Tour> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("estado"), TourStatus.ACTIVE));
            predicates.add(cb.equal(root.get("agencia").get("estado"), AgencyStatus.ACTIVE));
            if (filter.destination() != null && !filter.destination().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("destino")), "%" + filter.destination().trim().toLowerCase(java.util.Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%", '\\'));
            }
            if (filter.agencyId() != null) { predicates.add(cb.equal(root.get("agencia").get("id"), filter.agencyId())); }
            if (filter.experienceTypeId() != null) { predicates.add(cb.equal(root.get("tipoExperiencia").get("id"), filter.experienceTypeId())); }
            if (filter.difficulty() != null) { predicates.add(cb.equal(root.get("nivelDificultad"), filter.difficulty())); }
            if (filter.minPrice() != null) { predicates.add(cb.greaterThanOrEqualTo(root.get("precio"), filter.minPrice())); }
            if (filter.maxPrice() != null) { predicates.add(cb.lessThanOrEqualTo(root.get("precio"), filter.maxPrice())); }
            if (filter.minHours() != null) { predicates.add(cb.greaterThanOrEqualTo(root.get("duracionHoras"), filter.minHours())); }
            if (filter.maxHours() != null) { predicates.add(cb.lessThanOrEqualTo(root.get("duracionHoras"), filter.maxHours())); }
            if (filter.supportsCommunities() != null) { predicates.add(cb.equal(root.get("apoyoComunidades"), filter.supportsCommunities())); }
            if (filter.environmentalPractices() != null) { predicates.add(cb.equal(root.get("practicasAmbientales"), filter.environmentalPractices())); }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var result = tours.findAll(specification, PageRequest.of(page, size, Sort.by("precio").ascending().and(Sort.by("id"))));
        return new SearchResponse(result.map(this::entry).getContent(), result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public List<CatalogEntry> compare(List<Long> ids) {
        if (ids.size() < 2 || ids.size() > 6 || ids.stream().distinct().count() != ids.size()) {
            throw new ValidationException("Seleccione de 2 a 6 tours distintos para comparar.");
        }
        return ids.stream().map(id -> {
            Tour tour = tours.findById(id).filter(Tour::isActive).filter(t -> t.getAgencia().isActive())
                    .orElseThrow(() -> new ResourceNotFoundException("Uno de los tours no está disponible."));
            return entry(tour);
        }).toList();
    }

    @Transactional(readOnly = true)
    public TourDetails details(Long id) {
        Tour tour = tours.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tour no encontrado."));
        return details(tour);
    }

    @Transactional
    public TourDetails updateDetails(Long id, TourDetails request) {
        Tour tour = tours.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tour no encontrado."));
        tour.setItinerario(request.itinerary());
        tour.setCondiciones(request.conditions());
        tour.setApoyoComunidades(request.supportsCommunities());
        tour.setPracticasAmbientales(request.environmentalPractices());
        return details(tours.save(tour));
    }

    @Transactional(readOnly = true)
    public AgencyLocation location(Long id) {
        var agency = agencies.findById(id).orElseThrow(() -> new ResourceNotFoundException("Agencia no encontrada."));
        return new AgencyLocation(agency.getLatitud(), agency.getLongitud(), agency.getFotoUrl());
    }

    @Transactional
    public AgencyLocation updateLocation(Long id, AgencyLocation request) {
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new ValidationException("Latitud y longitud deben proporcionarse juntas.");
        }
        var agency = agencies.findById(id).orElseThrow(() -> new ResourceNotFoundException("Agencia no encontrada."));
        agency.setLatitud(request.latitude());
        agency.setLongitud(request.longitude());
        agency.setFotoUrl(request.photoUrl());
        agencies.save(agency);
        return request;
    }

    private TourDetails details(Tour tour) {
        return new TourDetails(tour.getItinerario(), tour.getCondiciones(), tour.isApoyoComunidades(), tour.isPracticasAmbientales());
    }

    private CatalogEntry entry(Tour tour) { return new CatalogEntry(TourMapper.toResponse(tour), details(tour)); }

    public record Filter(@Size(max = 120) String destination, @Positive Long agencyId, @Positive Long experienceTypeId,
                         DifficultyLevel difficulty, @DecimalMin("0.01") BigDecimal minPrice, @DecimalMin("0.01") BigDecimal maxPrice,
                         @DecimalMin("0.1") BigDecimal minHours, @DecimalMin("0.1") BigDecimal maxHours,
                         Boolean supportsCommunities, Boolean environmentalPractices) {}
    public record TourDetails(@Size(max = 10000) String itinerary, @Size(max = 10000) String conditions,
                              boolean supportsCommunities, boolean environmentalPractices) {}
    public record AgencyLocation(@DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
                                 @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
                                 @Size(max = 1500) @Pattern(regexp = "https://res\\.cloudinary\\.com/[a-zA-Z0-9_-]+/image/upload/[^\\s]+",
                                         message = "La foto debe ser una URL HTTPS de imagen de Cloudinary.") String photoUrl) {}
    public record CatalogEntry(TourResponse tour, TourDetails details) {}
    public record SearchResponse(List<CatalogEntry> content, long totalElements, int page, int size) {}
}
