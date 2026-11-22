package com.edu.unimag.rodatours.services.impl;

import java.util.Locale;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.api.dtos.request.CreateTouristRequest;
import com.edu.unimag.rodatours.api.dtos.request.UpdateTouristRequest;
import com.edu.unimag.rodatours.api.dtos.response.TouristResponse;
import com.edu.unimag.rodatours.domain.entities.Tourist;
import com.edu.unimag.rodatours.domain.entities.enums.TouristStatus;
import com.edu.unimag.rodatours.domain.repositories.TouristRepository;
import com.edu.unimag.rodatours.services.TouristService;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.mapper.TouristMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TouristServiceImpl implements TouristService {

    private final TouristRepository touristRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReservationRepository reservationRepository;

    @Override
    public TouristResponse create(CreateTouristRequest request) {
        String correo = normalizeEmail(request.correoElectronico());
        if (touristRepository.existsByCorreoElectronicoIgnoreCase(correo)) {
            throw new ConflictException("Ya existe un turista registrado con el correo: " + correo);
        }
        Tourist tourist = TouristMapper.toEntity(request);
        tourist.setCorreoElectronico(correo);
        tourist.setContrasena(passwordEncoder.encode(tourist.getContrasena()));
        Tourist saved = touristRepository.save(tourist);
        return TouristMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TouristResponse getById(Long id) {
        Tourist tourist = findTouristOrThrow(id);
        return TouristMapper.toResponse(tourist);
    }

    @Override
    @Transactional(readOnly = true)
    public TouristResponse getByEmail(String correoElectronico) {
        Tourist tourist = touristRepository.findByCorreoElectronicoIgnoreCase(correoElectronico)
                .orElseThrow(() -> new ResourceNotFoundException("Turista no encontrado con correo: " + correoElectronico));
        return TouristMapper.toResponse(tourist);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TouristResponse> getAll() {
        return touristRepository.findAll().stream()
                .map(TouristMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TouristResponse> getByStatus(TouristStatus estado) {
        return touristRepository.findByEstadoOrderByNombreCompletoAsc(estado).stream()
                .map(TouristMapper::toResponse)
                .toList();
    }

    @Override
    public TouristResponse update(Long id, UpdateTouristRequest request) {
        Tourist tourist = findTouristOrThrow(id);
        String correo = normalizeEmail(request.correoElectronico());
        if (!tourist.getCorreoElectronico().equalsIgnoreCase(correo)
                && touristRepository.existsByCorreoElectronicoIgnoreCase(correo)) {
            throw new ConflictException("Ya existe otro turista con el correo: " + correo);
        }
        TouristMapper.applyUpdate(request, tourist);
        tourist.setCorreoElectronico(correo);
        Tourist updated = touristRepository.save(tourist);
        return TouristMapper.toResponse(updated);
    }

    @Override
    public void delete(Long id) {
        Tourist tourist = findTouristOrThrow(id);
        if (reservationRepository.existsByTuristaId(id)) {
            throw new ConflictException("No se puede eliminar un turista con reservas asociadas. "
                    + "Desactive la cuenta cambiando su estado a INACTIVE.");
        }
        touristRepository.delete(tourist);
    }

    /** Los correos se guardan sin espacios y en minúsculas para que la unicidad no dependa de mayúsculas. */
    private static String normalizeEmail(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private Tourist findTouristOrThrow(Long id) {
        return touristRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turista no encontrado con id: " + id));
    }
}
