package com.edu.unimag.rodatours.media;

import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import com.edu.unimag.rodatours.services.exceptions.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class TourImageService {
    public static final long MAX_BYTES=5L*1024*1024;
    private final TourRepository tours;private final TourImageRepository images;private final ImageProvider provider;
    @Transactional(readOnly=true) public List<Item> list(Long tourId,boolean manage){
        var tour=tours.findById(tourId).orElseThrow(()->new ResourceNotFoundException("Tour no encontrado."));
        if(!manage&&(!tour.isActive()||!tour.getAgencia().isActive()))throw new ResourceNotFoundException("Tour no disponible.");
        return images.findByTourIdOrderByIdAsc(tourId).stream().filter(i->manage||"READY".equals(i.getStatus())).map(this::item).toList();
    }
    @Transactional public Upload begin(Long tourId){
        var tour=tours.findByIdForUpdate(tourId).orElseThrow(()->new ResourceNotFoundException("Tour no encontrado."));
        if(images.countByTourId(tourId)>=6)throw new ConflictException("Máximo 6 imágenes por tour, incluidas cargas pendientes. Elimine las pendientes si necesita espacio.");
        var image=new TourImage();image.setTour(tour);image.setPublicId("rodatours-image-"+UUID.randomUUID());image.setExpiresAt(Instant.now().plusSeconds(3600));
        var auth=provider.authorize(image.getPublicId());images.saveAndFlush(image);return new Upload(image.getId(),auth,image.getExpiresAt(),MAX_BYTES);
    }
    @Transactional public Item complete(Long tourId,Long id){
        var image=images.lock(tourId,id).orElseThrow(()->new ResourceNotFoundException("Imagen no encontrada."));
        if("READY".equals(image.getStatus()))return item(image);
        if(!image.getExpiresAt().isAfter(Instant.now()))throw new ConflictException("La carga venció. Elimínela antes de iniciar otra.");
        var metadata=provider.inspect(image.getPublicId());
        if(!Set.of("jpg","jpeg","png","webp").contains(metadata.format())||metadata.bytes()<=0||metadata.bytes()>MAX_BYTES||metadata.width()<1||metadata.height()<1||metadata.width()>12000||metadata.height()>12000||(long)metadata.width()*metadata.height()>40_000_000)
            throw new ValidationException("Imagen inválida: JPEG/PNG/WebP, máximo 5 MiB y dimensiones válidas.");
        image.setUrl(metadata.url());image.setStatus("READY");return item(images.save(image));
    }
    @Transactional public void delete(Long tourId,Long id){var image=images.lock(tourId,id).orElseThrow(()->new ResourceNotFoundException("Imagen no encontrada."));provider.delete(image.getPublicId());images.delete(image);}
    private Item item(TourImage i){return new Item(i.getId(),i.getStatus(),i.getUrl(),i.getExpiresAt());}
    public record Item(Long id,String status,String url,Instant expiresAt){}
    public record Upload(Long id,VideoProvider.UploadAuthorization upload,Instant expiresAt,long maxBytes){}
}
