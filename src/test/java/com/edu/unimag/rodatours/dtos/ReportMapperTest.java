package com.edu.unimag.rodatours.dtos;

import com.edu.unimag.rodatours.dtos.mapper.ReportMapper;
import com.edu.unimag.rodatours.dtos.response.AgencyOccupancyResponse;
import com.edu.unimag.rodatours.dtos.response.AgencyProductivityResponse;
import com.edu.unimag.rodatours.dtos.response.NoShowTouristResponse;
import com.edu.unimag.rodatours.repositories.projections.AgencyOccupancyProjection;
import com.edu.unimag.rodatours.repositories.projections.AgencyProductivityProjection;
import com.edu.unimag.rodatours.repositories.projections.NoShowTouristProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReportMapperTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 12, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 12, 31);

    @Test
    @DisplayName("El reporte de ocupación calcula el porcentaje con dos decimales")
    void reporteDeOcupacion() {
        AgencyOccupancyResponse response = ReportMapper.toOccupancyResponse(
                occupancy(1L, "Rodadero Tours", 15L, 5L), DESDE, HASTA);

        assertEquals(1L, response.agenciaId());
        assertEquals("Rodadero Tours", response.nombreComercial());
        assertEquals(DESDE, response.fechaDesde());
        assertEquals(HASTA, response.fechaHasta());
        assertEquals(15L, response.cuposTotales());
        assertEquals(5L, response.cuposOcupados());
        assertEquals(new BigDecimal("33.33"), response.porcentajeOcupacion());
    }

    @Test
    @DisplayName("El porcentaje se redondea HALF_UP y es cero cuando no hay cupos totales")
    void porcentajeRedondeadoYCero() {
        AgencyOccupancyResponse dosTercios = ReportMapper.toOccupancyResponse(
                occupancy(1L, "Agencia", 3L, 2L), DESDE, HASTA);
        assertEquals(new BigDecimal("66.67"), dosTercios.porcentajeOcupacion());

        AgencyOccupancyResponse sinTotales = ReportMapper.toOccupancyResponse(
                occupancy(2L, "Sin salidas", 0L, 0L), DESDE, HASTA);
        assertEquals(new BigDecimal("0.00"), sinTotales.porcentajeOcupacion());

        AgencyOccupancyResponse conNulos = ReportMapper.toOccupancyResponse(
                occupancy(3L, "Nulos", null, null), DESDE, HASTA);
        assertEquals(0L, conNulos.cuposTotales());
        assertEquals(0L, conNulos.cuposOcupados());
        assertEquals(new BigDecimal("0.00"), conNulos.porcentajeOcupacion());
    }

    @Test
    @DisplayName("El reporte de productividad conserva el puesto del ranking")
    void reporteDeProductividad() {
        AgencyProductivityResponse response = ReportMapper.toProductivityResponse(
                productivity(4L, "Aventura Tours", 12L), 2);

        assertEquals(4L, response.agenciaId());
        assertEquals("Aventura Tours", response.nombreComercial());
        assertEquals(12L, response.toursCompletados());
        assertEquals(2, response.posicion());
    }

    @Test
    @DisplayName("El reporte de inasistencias incluye el período consultado")
    void reporteDeInasistencias() {
        NoShowTouristResponse response = ReportMapper.toNoShowResponse(
                noShow(5L, "Carlos Ruiz", 3L), DESDE, HASTA);

        assertEquals(5L, response.turistaId());
        assertEquals("Carlos Ruiz", response.nombreCompleto());
        assertEquals(3L, response.cantidadNoShow());
        assertEquals(DESDE, response.fechaDesde());
        assertEquals(HASTA, response.fechaHasta());
    }

    @Test
    @DisplayName("Los mappers de reporte devuelven null cuando la proyección no existe")
    void proyeccionesNulas() {
        assertNull(ReportMapper.toOccupancyResponse(null, DESDE, HASTA));
        assertNull(ReportMapper.toProductivityResponse(null, 1));
        assertNull(ReportMapper.toNoShowResponse(null, DESDE, HASTA));
    }

    private AgencyOccupancyProjection occupancy(Long id, String nombre, Long totales, Long ocupados) {
        return new AgencyOccupancyProjection() {
            @Override
            public Long getAgenciaId() {
                return id;
            }

            @Override
            public String getNombreComercial() {
                return nombre;
            }

            @Override
            public Long getCuposTotales() {
                return totales;
            }

            @Override
            public Long getCuposOcupados() {
                return ocupados;
            }
        };
    }

    private AgencyProductivityProjection productivity(Long id, String nombre, Long completados) {
        return new AgencyProductivityProjection() {
            @Override
            public Long getAgenciaId() {
                return id;
            }

            @Override
            public String getNombreComercial() {
                return nombre;
            }

            @Override
            public Long getToursCompletados() {
                return completados;
            }
        };
    }

    private NoShowTouristProjection noShow(Long id, String nombre, Long cantidad) {
        return new NoShowTouristProjection() {
            @Override
            public Long getTuristaId() {
                return id;
            }

            @Override
            public String getNombreCompleto() {
                return nombre;
            }

            @Override
            public Long getCantidadNoShow() {
                return cantidad;
            }
        };
    }
}
