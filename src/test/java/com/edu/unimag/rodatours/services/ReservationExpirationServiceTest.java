package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.domain.entities.*;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.history.ReservationStateChanged;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationExpirationServiceTest {
    @Test void onlyExpiredPendingReservationsAreCancelledWithoutIncreasingPhysicalStock(){
        var repo=mock(ReservationRepository.class);var events=mock(ApplicationEventPublisher.class);var service=new ReservationExpirationService(repo,events);
        var schedule=TourSchedule.builder().tour(Tour.builder().agencia(Agency.builder().id(1L).build()).build()).cuposTotales(10).cuposDisponibles(10).build();
        var r=Reservation.builder().id(1L).fechaReserva(LocalDateTime.now().minusHours(2)).turista(Tourist.builder().correoElectronico("a@example.com").build()).horario(schedule).build();
        when(repo.findByIdForUpdate(1L)).thenReturn(Optional.of(r));service.expire(1L,LocalDateTime.now().minusHours(1));assertEquals(ReservationStatus.CANCELLED,r.getEstado());assertEquals(10,schedule.getCuposDisponibles());verify(events).publishEvent(any(ReservationStateChanged.class));
        clearInvocations(repo,events);r.setEstado(ReservationStatus.CONFIRMED);service.expire(1L,LocalDateTime.now());verifyNoInteractions(events);verify(repo,never()).save(any());
    }
}
