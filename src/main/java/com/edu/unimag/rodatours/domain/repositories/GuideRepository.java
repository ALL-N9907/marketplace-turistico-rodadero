package com.edu.unimag.rodatours.domain.repositories;

import com.edu.unimag.rodatours.domain.entities.Guide;
import com.edu.unimag.rodatours.domain.entities.enums.GuideStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Consultas del catálogo de guías y de sus salidas asignadas.
 */
public interface GuideRepository extends JpaRepository<Guide, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select g from Guide g where g.id = :id")
    Optional<Guide> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<Guide> findByDocumentoIdentidad(String documentoIdentidad);

    Optional<Guide> findByRegistroGuia(String registroGuia);

    Optional<Guide> findByCorreoElectronicoIgnoreCase(String correoElectronico);

    boolean existsByDocumentoIdentidad(String documentoIdentidad);
    boolean existsByAgenciaId(Long agenciaId);

    boolean existsByRegistroGuia(String registroGuia);

    boolean existsByCorreoElectronicoIgnoreCase(String correoElectronico);

    List<Guide> findByAgenciaIdAndEstado(Long agenciaId, GuideStatus estado);
}
