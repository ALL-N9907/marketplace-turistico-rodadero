package com.edu.unimag.rodatours.repositories;

import com.edu.unimag.rodatours.entities.Guide;
import com.edu.unimag.rodatours.entities.enums.GuideStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Consultas del catálogo de guías y de sus salidas asignadas.
 */
public interface GuideRepository extends JpaRepository<Guide, Long> {

    Optional<Guide> findByDocumentoIdentidad(String documentoIdentidad);

    Optional<Guide> findByRegistroGuia(String registroGuia);

    Optional<Guide> findByCorreoElectronicoIgnoreCase(String correoElectronico);

    boolean existsByDocumentoIdentidad(String documentoIdentidad);

    boolean existsByRegistroGuia(String registroGuia);

    boolean existsByCorreoElectronicoIgnoreCase(String correoElectronico);

    List<Guide> findByAgenciaIdAndEstado(Long agenciaId, GuideStatus estado);
}
