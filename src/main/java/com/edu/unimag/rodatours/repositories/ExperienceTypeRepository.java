package com.edu.unimag.rodatours.repositories;

import com.edu.unimag.rodatours.entities.ExperienceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Consultas derivadas sobre el catálogo de tipos de experiencia.
 */
public interface ExperienceTypeRepository extends JpaRepository<ExperienceType, Long> {

    Optional<ExperienceType> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    List<ExperienceType> findAllByOrderByNombreAsc();
}
