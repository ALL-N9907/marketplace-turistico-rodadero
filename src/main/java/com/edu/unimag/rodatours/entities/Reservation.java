package com.edu.unimag.rodatours.entities;

import com.edu.unimag.rodatours.entities.enums.ReservationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(
    name = "reservations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_reservations_codigo_qr", columnNames = "codigo_qr")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotNull(message = "El turista es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turista_id", nullable = false)
    private Tourist turista;

    @NotNull(message = "El horario de salida es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horario_id", nullable = false)
    private TourSchedule horario;

    @Column(name = "fecha_reserva", nullable = false, updatable = false)
    @ToString.Include
    private LocalDateTime fechaReserva;

    @NotNull(message = "La fecha y hora de inicio son obligatorias")
    @Column(name = "fecha_inicio", nullable = false)
    @ToString.Include
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha y hora de finalización son obligatorias")
    @Column(name = "fecha_fin", nullable = false)
    @ToString.Include
    private LocalDateTime fechaFin;

    @NotNull(message = "La cantidad de personas es obligatoria")
    @Min(value = 1, message = "Debe reservarse al menos para 1 persona")
    @Column(name = "cantidad_personas", nullable = false)
    @ToString.Include
    private Integer cantidadPersonas;

    @NotNull(message = "El precio total es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio total debe ser mayor que cero")
    @Column(name = "precio_total", nullable = false, precision = 12, scale = 2)
    @ToString.Include
    private BigDecimal precioTotal;

    @NotNull(message = "El estado de la reserva es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    @ToString.Include
    private ReservationStatus estado = ReservationStatus.SCHEDULED;

    @Column(name = "motivo_cancelacion", columnDefinition = "TEXT")
    private String motivoCancelacion;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Size(max = 100, message = "El código QR no puede superar los 100 caracteres")
    @Column(name = "codigo_qr", unique = true, length = 100)
    @ToString.Include
    private String codigoQr;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @OneToOne(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Review review;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.fechaReserva == null) {
            this.fechaReserva = now;
        }
        if (this.estado == null) {
            this.estado = ReservationStatus.SCHEDULED;
        }
        if (this.fechaModificacion == null) {
            this.fechaModificacion = now;
        }
        if (this.codigoQr == null || this.codigoQr.isBlank()) {
            this.codigoQr = UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }


    public void confirm() {
        if (!ReservationStatus.SCHEDULED.equals(this.estado)) {
            throw new IllegalStateException("Solo una reserva en estado SCHEDULED puede confirmarse. Estado actual: " + this.estado);
        }
        this.estado = ReservationStatus.CONFIRMED;
        this.fechaModificacion = LocalDateTime.now();
    }


    public void cancel(String motivo) {
        if (this.estado == null || (!this.estado.equals(ReservationStatus.SCHEDULED) && !this.estado.equals(ReservationStatus.CONFIRMED))) {
            throw new IllegalStateException("Solo reservas en estado SCHEDULED o CONFIRMED pueden cancelarse. Estado actual: " + this.estado);
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de cancelación es obligatorio.");
        }
        this.estado = ReservationStatus.CANCELLED;
        this.motivoCancelacion = motivo.trim();
        this.fechaModificacion = LocalDateTime.now();
    }


    public void complete(String observacionesAdicionales) {
        if (!ReservationStatus.CONFIRMED.equals(this.estado)) {
            throw new IllegalStateException("Solo una reserva en estado CONFIRMED puede completarse. Estado actual: " + this.estado);
        }
        this.estado = ReservationStatus.COMPLETED;
        if (observacionesAdicionales != null && !observacionesAdicionales.trim().isEmpty()) {
            this.observaciones = (this.observaciones == null || this.observaciones.isBlank())
                    ? observacionesAdicionales.trim()
                    : this.observaciones + " | " + observacionesAdicionales.trim();
        }
        this.fechaModificacion = LocalDateTime.now();
    }


    public void markNoShow() {
        if (!ReservationStatus.CONFIRMED.equals(this.estado)) {
            throw new IllegalStateException("Solo una reserva en estado CONFIRMED puede marcarse como NO_SHOW. Estado actual: " + this.estado);
        }
        this.estado = ReservationStatus.NO_SHOW;
        this.fechaModificacion = LocalDateTime.now();
    }
}
