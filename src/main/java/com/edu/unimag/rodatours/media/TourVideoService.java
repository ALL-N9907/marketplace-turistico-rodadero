package com.edu.unimag.rodatours.media;

import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourVideoService {
    private final TourVideoRepository videos;
    private final TourRepository tours;
    private final VideoProvider provider;
    private final CloudinaryProperties properties;

    @Transactional
    public UploadResponse begin(Long tourId) {
        var tour = tours.findById(tourId).orElseThrow(() -> new ResourceNotFoundException("Tour no encontrado."));
        if (!tour.isActive()) {
            throw new ValidationException("El tour debe estar activo para cargar un video.");
        }
        if (videos.existsById(tourId)) {
            throw new ConflictException("El tour ya tiene una carga o video. Elimínelo antes de reemplazarlo.");
        }
        Instant issuedAt = Instant.now();
        String publicId = "rodatours-tour-" + tourId + "-" + UUID.randomUUID();
        var authorization = provider.authorize(publicId, issuedAt);
        TourVideo video = new TourVideo();
        video.setTour(tour);
        video.setPublicId(publicId);
        video.setIssuedAt(issuedAt);
        videos.saveAndFlush(video);
        return new UploadResponse(authorization, issuedAt.plusSeconds(3600), properties.getMaxBytes(),
                properties.getMaxDurationSeconds());
    }

    @Transactional
    public VideoResponse complete(Long tourId) {
        var video = videos.findByIdForUpdate(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Carga de video no encontrada."));
        if (video.isReady()) {
            return response(video);
        }
        if (Instant.now().isAfter(video.getIssuedAt().plusSeconds(3600))) {
            throw new ConflictException("La carga expiró. Elimínela e inicie una nueva.");
        }
        var metadata = provider.inspect(video.getPublicId());
        if (!video.getPublicId().equals(metadata.publicId()) || !Set.of("mp4", "webm", "mov").contains(metadata.format())
                || metadata.bytes() <= 0 || metadata.bytes() > properties.getMaxBytes()
                || !Double.isFinite(metadata.duration()) || metadata.duration() <= 0
                || metadata.duration() > properties.getMaxDurationSeconds() || metadata.width() <= 0 || metadata.height() <= 0) {
            throw new ValidationException("El archivo debe ser un video MP4, WebM o MOV dentro de los límites de tamaño y duración.");
        }
        video.setSecureUrl(metadata.secureUrl());
        video.setFormat(metadata.format());
        video.setBytes(metadata.bytes());
        video.setDurationSeconds(metadata.duration());
        video.setWidth(metadata.width());
        video.setHeight(metadata.height());
        video.setReady(true);
        return response(videos.save(video));
    }

    @Transactional(readOnly = true)
    public VideoResponse get(Long tourId) {
        TourVideo video = videos.findById(tourId).filter(TourVideo::isReady)
                .orElseThrow(() -> new ResourceNotFoundException("Video publicado no encontrado."));
        if (!video.getTour().isActive() || !video.getTour().getAgencia().isActive()) {
            throw new ResourceNotFoundException("Video publicado no encontrado.");
        }
        return response(video);
    }

    @Transactional
    public void delete(Long tourId) {
        var video = videos.findByIdForUpdate(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Video o carga no encontrados."));
        provider.delete(video.getPublicId());
        videos.delete(video);
        videos.flush();
    }

    private VideoResponse response(TourVideo video) {
        return new VideoResponse(video.getTourId(), video.getSecureUrl(), video.getFormat(), video.getBytes(),
                video.getDurationSeconds(), video.getWidth(), video.getHeight());
    }

    public record UploadResponse(VideoProvider.UploadAuthorization upload, Instant expiresAt,
                                 long maxBytes, double maxDurationSeconds) {}
    public record VideoResponse(Long tourId, String url, String format, Long bytes,
                                Double durationSeconds, Integer width, Integer height) {}
}
