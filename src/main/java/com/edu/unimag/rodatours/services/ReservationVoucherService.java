package com.edu.unimag.rodatours.services;

import com.edu.unimag.rodatours.domain.entities.Reservation;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationVoucherService {
    private final ReservationRepository reservations;

    @Transactional(readOnly = true)
    public Voucher get(Long id) {
        return response(reservations.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada.")));
    }

    @Transactional(readOnly = true)
    public byte[] qr(Long id) {
        String payload = get(id).qrPayload();
        try {
            var matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 320, 320);
            var image = new BufferedImage(320, 320, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 320; y++) {
                for (int x = 0; x < 320; x++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            var output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el QR.", exception);
        }
    }

    @Transactional
    public Voucher checkIn(Long id, String qrPayload) {
        Reservation reservation = reservations.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada."));
        if (!("rodatours:reservation:" + reservation.getCodigoQr()).equals(qrPayload)) {
            throw new ValidationException("El QR no corresponde a la reserva.");
        }
        if (reservation.getEstado() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("Sólo se puede registrar asistencia de una reserva confirmada.");
        }
        if (!LocalDate.now().equals(reservation.getHorario().getFechaTour())) {
            throw new ConflictException("El QR sólo puede canjearse el día de la salida.");
        }
        if (reservation.getFechaCheckIn() != null) {
            throw new ConflictException("El QR de la reserva ya fue canjeado.");
        }
        reservation.setFechaCheckIn(LocalDateTime.now());
        return response(reservations.save(reservation));
    }

    private Voucher response(Reservation reservation) {
        return new Voucher(reservation.getId(), reservation.getHorario().getTour().getTitulo(),
                reservation.getHorario().getTour().getAgencia().getNombreComercial(), reservation.getFechaInicio(),
                reservation.getFechaFin(), reservation.getCantidadPersonas(), reservation.getPrecioTotal(),
                reservation.getEstado(), "rodatours:reservation:" + reservation.getCodigoQr(), reservation.getFechaCheckIn());
    }

    public record Voucher(Long reservationId, String tour, String agency, LocalDateTime start,
                          LocalDateTime end, Integer persons, BigDecimal total, ReservationStatus status,
                          String qrPayload, LocalDateTime checkedInAt) {}
}
