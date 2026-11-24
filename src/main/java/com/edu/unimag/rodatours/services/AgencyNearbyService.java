package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.api.dtos.response.AgencyResponse;
import com.edu.unimag.rodatours.domain.entities.enums.AgencyStatus;
import com.edu.unimag.rodatours.domain.repositories.AgencyRepository;
import com.edu.unimag.rodatours.services.mapper.AgencyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;

@Service @RequiredArgsConstructor
public class AgencyNearbyService {
    private final AgencyRepository agencies;
    @Transactional(readOnly=true)
    public List<Nearby> search(double latitude,double longitude,double radiusKm){
        if(!Double.isFinite(latitude)||!Double.isFinite(longitude)||!Double.isFinite(radiusKm)||Math.abs(latitude)>90||Math.abs(longitude)>180||radiusKm<0.1||radiusKm>200)
            throw new com.edu.unimag.rodatours.services.exceptions.ValidationException("Coordenadas o radio inválidos.");
        return agencies.findByEstadoOrderByNombreComercialAsc(AgencyStatus.ACTIVE).stream()
                .filter(a->a.getLatitud()!=null&&a.getLongitud()!=null)
                .map(a->new Nearby(AgencyMapper.toResponse(a),distance(latitude,longitude,a.getLatitud().doubleValue(),a.getLongitud().doubleValue())))
                .filter(a->a.distanceKm()<=radiusKm).sorted(Comparator.comparingDouble(Nearby::distanceKm).thenComparing(a->a.agency().id()))
                .limit(50).toList();
    }
    public static double distance(double lat1,double lon1,double lat2,double lon2){
        double dLat=Math.toRadians(lat2-lat1),dLon=Math.toRadians(lon2-lon1);
        double a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        return 6371.0088*2*Math.asin(Math.sqrt(Math.max(0,Math.min(1,a))));
    }
    public record Nearby(AgencyResponse agency,double distanceKm){}
}
