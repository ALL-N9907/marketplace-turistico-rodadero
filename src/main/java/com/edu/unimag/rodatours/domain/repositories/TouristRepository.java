package com.edu.unimag.rodatours.domain.repositories;

import com.edu.unimag.rodatours.domain.entities.Tourist;
import com.edu.unimag.rodatours.domain.entities.enums.TouristStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Consultas derivadas sobre el catálogo de turistas.
 */
public interface TouristRepository extends JpaRepository<Tourist, Long> {

    Optional<Tourist> findByCorreoElectronico(String correoElectronico);

    Optional<Tourist> findByCorreoElectronicoIgnoreCase(String correoElectronico);

    boolean existsByCorreoElectronico(String correoElectronico);

    boolean existsByCorreoElectronicoIgnoreCase(String correoElectronico);

    List<Tourist> findByEstado(TouristStatus estado);

    List<Tourist> findByEstadoOrderByNombreCompletoAsc(TouristStatus estado);

    List<Tourist> findByEstadoAndNombreCompletoContainingIgnoreCase(TouristStatus estado, String nombre);

    @Query("select t from Tourist t join t.reservas r where r.id = :reservaId")
    Optional<Tourist> findByReservaId(@Param("reservaId") Long reservaId);
}
