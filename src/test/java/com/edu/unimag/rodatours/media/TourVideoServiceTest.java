package com.edu.unimag.rodatours.media;

import com.edu.unimag.rodatours.domain.entities.Agency;
import com.edu.unimag.rodatours.domain.entities.Tour;
import com.edu.unimag.rodatours.domain.entities.enums.TourStatus;
import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourVideoServiceTest {
    @Mock TourVideoRepository videos;
    @Mock TourRepository tours;
    @Mock VideoProvider provider;
    private TourVideoService service;
    private TourVideo video;
    private Tour tour;

    @BeforeEach
    void setup() {
        service = new TourVideoService(videos, tours, provider, new CloudinaryProperties());
        tour = Tour.builder().id(1L).agencia(Agency.builder().id(2L).build()).build();
        video = new TourVideo();
        video.setTour(tour);
        video.setTourId(1L);
        video.setPublicId("owned-video");
        video.setIssuedAt(Instant.now());
    }

    @Test
    void beginsBoundedUploadForExistingTour() {
        when(tours.findById(1L)).thenReturn(Optional.of(tour));
        when(provider.authorize(anyString(), any())).thenReturn(new VideoProvider.UploadAuthorization("url", "key", Map.of(), "signature"));
        var result = service.begin(1L);
        assertEquals(52_428_800, result.maxBytes());
        assertEquals(120, result.maxDurationSeconds());
        verify(videos).saveAndFlush(argThat(v -> v.getPublicId().startsWith("rodatours-tour-1-") && !v.isReady()));
    }

    @Test
    void rejectsDuplicateUpload() {
        when(tours.findById(1L)).thenReturn(Optional.of(tour));
        when(videos.existsById(1L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.begin(1L));
        verifyNoInteractions(provider);
    }

    @Test
    void rejectsInactiveTour() {
        tour.setEstado(TourStatus.INACTIVE);
        when(tours.findById(1L)).thenReturn(Optional.of(tour));
        assertThrows(ValidationException.class, () -> service.begin(1L));
        verifyNoInteractions(provider);
    }

    @Test
    void completesOnlyWithProviderVerifiedVideo() {
        when(videos.findByIdForUpdate(1L)).thenReturn(Optional.of(video));
        when(provider.inspect("owned-video")).thenReturn(new VideoProvider.VideoMetadata("owned-video", "https://video", "mp4", 2000, 30, 1280, 720));
        when(videos.save(video)).thenReturn(video);
        var result = service.complete(1L);
        assertEquals("https://video", result.url());
        assertTrue(video.isReady());
    }

    @Test
    void rejectsOversizedOrAudioOnlyAsset() {
        when(videos.findByIdForUpdate(1L)).thenReturn(Optional.of(video));
        when(provider.inspect("owned-video")).thenReturn(new VideoProvider.VideoMetadata("owned-video", "https://video", "mp4", 60_000_000, 30, 1280, 720));
        assertThrows(ValidationException.class, () -> service.complete(1L));
        when(provider.inspect("owned-video")).thenReturn(new VideoProvider.VideoMetadata("owned-video", "https://video", "mp4", 2000, 30, 0, 0));
        assertThrows(ValidationException.class, () -> service.complete(1L));
        verify(videos, never()).save(any());
    }

    @Test
    void rejectsTooLongOrWrongPublicId() {
        when(videos.findByIdForUpdate(1L)).thenReturn(Optional.of(video));
        when(provider.inspect("owned-video")).thenReturn(new VideoProvider.VideoMetadata("owned-video", "url", "mp4", 2000, 121, 1280, 720));
        assertThrows(ValidationException.class, () -> service.complete(1L));
        when(provider.inspect("owned-video")).thenReturn(new VideoProvider.VideoMetadata("foreign-video", "url", "mp4", 2000, 30, 1280, 720));
        assertThrows(ValidationException.class, () -> service.complete(1L));
    }

    @Test
    void refusesExpiredSessionWithoutContactingCloudinary() {
        video.setIssuedAt(Instant.now().minusSeconds(3700));
        when(videos.findByIdForUpdate(1L)).thenReturn(Optional.of(video));
        assertThrows(ConflictException.class, () -> service.complete(1L));
        verifyNoInteractions(provider);
    }

    @Test
    void doesNotExposePendingVideo() {
        when(videos.findById(1L)).thenReturn(Optional.of(video));
        assertThrows(ResourceNotFoundException.class, () -> service.get(1L));
    }

    @Test
    void incompleteMetadataKeepsPendingUploadWithoutPublishingOrDeleting() {
        when(videos.findByIdForUpdate(1L)).thenReturn(Optional.of(video));
        when(provider.inspect("owned-video")).thenThrow(new ConflictException("Metadatos pendientes"));
        assertThrows(ConflictException.class, () -> service.complete(1L));
        assertFalse(video.isReady());
        verify(videos, never()).save(any());
        verify(videos, never()).delete(any());
        verify(provider, never()).delete(anyString());
    }

    @Test
    void preservesDatabaseRecordWhenProviderDeletionFails() {
        when(videos.findByIdForUpdate(1L)).thenReturn(Optional.of(video));
        doThrow(new MediaProviderException("No disponible")).when(provider).delete("owned-video");
        assertThrows(MediaProviderException.class, () -> service.delete(1L));
        verify(videos, never()).delete(any());
    }

    @Test
    void deletesCloudAssetBeforeRemovingRecord() {
        when(videos.findByIdForUpdate(1L)).thenReturn(Optional.of(video));
        service.delete(1L);
        var order = inOrder(provider, videos);
        order.verify(provider).delete("owned-video");
        order.verify(videos).delete(video);
    }
}
