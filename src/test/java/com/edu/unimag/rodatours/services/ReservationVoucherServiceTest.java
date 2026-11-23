package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.domain.entities.*;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationVoucherServiceTest {
    @Mock ReservationRepository reservations;
    @InjectMocks ReservationVoucherService service;
    private Reservation reservation;

    @BeforeEach
    void setup() {
        var agency = Agency.builder().nombreComercial("Roda Tours").build();
        var tour = Tour.builder().titulo("Minca").agencia(agency).build();
        var schedule = TourSchedule.builder().tour(tour).fechaTour(LocalDate.now()).build();
        reservation = Reservation.builder().id(1L).horario(schedule).codigoQr("unique-code")
                .estado(ReservationStatus.CONFIRMED).build();
    }

    @Test
    void generatesPngThatDecodesToReservationToken() throws Exception {
        when(reservations.findById(1L)).thenReturn(Optional.of(reservation));
        byte[] png = service.qr(1L);
        var image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(320, image.getWidth());
        int[] pixels = image.getRGB(0, 0, 320, 320, null, 0, 320);
        var source = new RGBLuminanceSource(320, 320, pixels);
        var result = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)));
        assertEquals("rodatours:reservation:unique-code", result.getText());
    }

    @Test
    void canRedeemOnlyOnce() {
        when(reservations.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));
        when(reservations.save(reservation)).thenReturn(reservation);
        assertNotNull(service.checkIn(1L, "rodatours:reservation:unique-code").checkedInAt());
        assertThrows(ConflictException.class, () -> service.checkIn(1L, "rodatours:reservation:unique-code"));
    }

    @Test
    void rejectsWrongCode() {
        when(reservations.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));
        assertThrows(ValidationException.class, () -> service.checkIn(1L, "wrong-code"));
        verify(reservations, never()).save(any());
    }

    @Test
    void rejectsFutureDateAndCancelledReservation() {
        when(reservations.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));
        reservation.getHorario().setFechaTour(LocalDate.now().plusDays(1));
        assertThrows(ConflictException.class, () -> service.checkIn(1L, "rodatours:reservation:unique-code"));
        reservation.getHorario().setFechaTour(LocalDate.now());
        reservation.setEstado(ReservationStatus.CANCELLED);
        assertThrows(ConflictException.class, () -> service.checkIn(1L, "rodatours:reservation:unique-code"));
    }
}
