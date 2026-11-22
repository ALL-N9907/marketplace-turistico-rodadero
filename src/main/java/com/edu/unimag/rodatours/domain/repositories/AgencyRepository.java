package com.edu.unimag.rodatours.domain.repositories;

import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.enums.AgencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Búsqueda de agencias activas por nombre o ubicación.
 */
public interface AgencyRepository extends JpaRepository<Agency, Long> {

    Optional<Agency> findByNitORegistro(String nitORegistro);

    boolean existsByNitORegistro(String nitORegistro);

    List<Agency> findByEstadoAndNombreComercialContainingIgnoreCase(AgencyStatus estado, String nombre);

    List<Agency> findByEstadoAndDireccionContainingIgnoreCase(AgencyStatus estado, String direccion);

    List<Agency> findByEstadoOrderByNombreComercialAsc(AgencyStatus estado);

    @Query("""
            select a from Agency a
            where a.estado = :estado
              and (lower(a.nombreComercial) like lower(concat('%', :texto, '%'))
                   or lower(a.direccion) like lower(concat('%', :texto, '%')))
            order by a.nombreComercial asc
            """)
    List<Agency> searchActiveByNameOrLocation(@Param("estado") AgencyStatus estado,
                                              @Param("texto") String texto);
}
