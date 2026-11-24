package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.enums.AgencyStatus;
import com.edu.unimag.rodatours.domain.repositories.AgencyRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgencyNearbyServiceTest {
    @Test void haversineUsesKilometersAndHandlesIdenticalCoordinates(){assertEquals(0,AgencyNearbyService.distance(11,-74,11,-74),0.00001);assertEquals(111.195,AgencyNearbyService.distance(0,0,0,1),0.01);}
    @Test void onlyLocatedActiveAgenciesWithinRadiusAreReturned(){
        var repo=mock(AgencyRepository.class);var near=Agency.builder().id(1L).nombreComercial("Cerca").build();near.setLatitud(BigDecimal.valueOf(11));near.setLongitud(BigDecimal.valueOf(-74));
        var far=Agency.builder().id(2L).nombreComercial("Lejos").build();far.setLatitud(BigDecimal.valueOf(12));far.setLongitud(BigDecimal.valueOf(-74));
        var unknown=Agency.builder().id(3L).nombreComercial("Sin coordenadas").build();
        when(repo.findByEstadoOrderByNombreComercialAsc(AgencyStatus.ACTIVE)).thenReturn(List.of(near,far,unknown));
        var result=new AgencyNearbyService(repo).search(11,-74,10);assertEquals(1,result.size());assertEquals(1L,result.getFirst().agency().id());
    }
}
