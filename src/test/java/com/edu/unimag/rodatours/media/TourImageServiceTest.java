package com.edu.unimag.rodatours.media;

import com.edu.unimag.rodatours.domain.entities.*;
import com.edu.unimag.rodatours.domain.repositories.TourRepository;
import com.edu.unimag.rodatours.services.exceptions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourImageServiceTest {
    @Mock TourRepository tours;@Mock TourImageRepository images;@Mock ImageProvider provider;@InjectMocks TourImageService service;
    private TourImage image(){var i=new TourImage();i.setId(2L);i.setPublicId("image-test");i.setExpiresAt(Instant.now().plusSeconds(60));return i;}
    @Test void pendingUploadsCountAgainstLimit(){when(tours.findByIdForUpdate(1L)).thenReturn(Optional.of(Tour.builder().id(1L).build()));when(images.countByTourId(1L)).thenReturn(6L);assertThrows(ConflictException.class,()->service.begin(1L));verifyNoInteractions(provider);}
    @Test void neverPublishesOversizedOrWrongFormatImages(){var i=image();when(images.lock(1L,2L)).thenReturn(Optional.of(i));when(provider.inspect("image-test")).thenReturn(new ImageProvider.ImageMetadata("https://res.cloudinary.com/test/image/upload/a.gif","gif",100,100,100));assertThrows(ValidationException.class,()->service.complete(1L,2L));assertEquals("PENDING",i.getStatus());verify(images,never()).save(any());}
    @Test void publishesOnlyAfterVerification(){var i=image();when(images.lock(1L,2L)).thenReturn(Optional.of(i));when(provider.inspect("image-test")).thenReturn(new ImageProvider.ImageMetadata("https://res.cloudinary.com/test/image/upload/a.jpg","jpg",1000,800,600));when(images.save(i)).thenReturn(i);assertEquals("READY",service.complete(1L,2L).status());}
    @Test void providerDeleteFailurePreservesRecordForRetry(){var i=image();when(images.lock(1L,2L)).thenReturn(Optional.of(i));doThrow(new MediaProviderException("fallo")).when(provider).delete("image-test");assertThrows(MediaProviderException.class,()->service.delete(1L,2L));verify(images,never()).delete(any());}
    @Test void publicListExcludesUnverifiedUploads(){when(tours.findById(1L)).thenReturn(Optional.of(Tour.builder().agencia(Agency.builder().build()).build()));when(images.findByTourIdOrderByIdAsc(1L)).thenReturn(List.of(image()));assertTrue(service.list(1L,false).isEmpty());assertEquals(1,service.list(1L,true).size());}
}
