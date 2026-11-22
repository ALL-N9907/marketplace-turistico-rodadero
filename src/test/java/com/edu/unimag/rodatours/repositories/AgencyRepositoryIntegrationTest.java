package com.edu.unimag.rodatours.repositories;

import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.enums.AgencyStatus;
import com.edu.unimag.rodatours.domain.repositories.AgencyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgencyRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

    @Autowired
    private AgencyRepository repository;

    @Test
    @DisplayName("Busca agencias por NIT o registro")
    void findByNitORegistro() {
        Agency agency = TestDataFactory.agency(entityManager, "tayrona");

        assertEquals(agency.getId(), repository.findByNitORegistro("NIT-tayrona").orElseThrow().getId());
        assertTrue(repository.existsByNitORegistro("NIT-tayrona"));
        assertTrue(repository.findByNitORegistro("NIT-inexistente").isEmpty());
    }

    @Test
    @DisplayName("Busca agencias activas por nombre comercial ignorando mayúsculas")
    void findByEstadoAndNombreComercial() {
        TestDataFactory.agency(entityManager, "rodadero", AgencyStatus.ACTIVE);
        TestDataFactory.agency(entityManager, "rodadero-inactiva", AgencyStatus.INACTIVE);

        List<Agency> encontradas = repository.findByEstadoAndNombreComercialContainingIgnoreCase(
                AgencyStatus.ACTIVE, "RODADERO");

        assertEquals(1, encontradas.size());
        assertEquals("Agencia rodadero", encontradas.getFirst().getNombreComercial());
    }

    @Test
    @DisplayName("Busca agencias activas por ubicación")
    void findByEstadoAndDireccion() {
        Agency agency = TestDataFactory.agency(entityManager, "centro");

        List<Agency> encontradas = repository.findByEstadoAndDireccionContainingIgnoreCase(
                AgencyStatus.ACTIVE, "EL RODADERO");

        assertEquals(1, encontradas.size());
        assertEquals(agency.getId(), encontradas.getFirst().getId());
        assertTrue(repository.findByEstadoAndDireccionContainingIgnoreCase(
                AgencyStatus.ACTIVE, "Santa Marta").isEmpty());
    }

    @Test
    @DisplayName("La búsqueda combinada devuelve agencias activas que coinciden por nombre o dirección")
    void searchActiveByNameOrLocation() {
        Agency porNombre = TestDataFactory.agency(entityManager, "aventura");
        Agency porDireccion = TestDataFactory.agency(entityManager, "norte");
        TestDataFactory.agency(entityManager, "oculta", AgencyStatus.INACTIVE);

        List<Agency> encontradas = repository.searchActiveByNameOrLocation(AgencyStatus.ACTIVE, "aventura");
        assertEquals(1, encontradas.size());
        assertEquals(porNombre.getId(), encontradas.getFirst().getId());

        List<Agency> porUbicacion = repository.searchActiveByNameOrLocation(AgencyStatus.ACTIVE, "CALLE norte");
        assertEquals(1, porUbicacion.size());
        assertEquals(porDireccion.getId(), porUbicacion.getFirst().getId());
        assertTrue(repository.searchActiveByNameOrLocation(AgencyStatus.ACTIVE, "inexistente").isEmpty());
    }

    @Test
    @DisplayName("Rechaza agencias con NIT duplicado")
    void nitUnico() {
        Agency existente = TestDataFactory.agency(entityManager, "duplicada");
        Agency duplicada = Agency.builder()
                .nombreComercial("Otra agencia")
                .nitORegistro(existente.getNitORegistro())
                .direccion("Calle 1")
                .telefonoContacto("3001000001")
                .estado(AgencyStatus.ACTIVE)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicada));
    }
}
