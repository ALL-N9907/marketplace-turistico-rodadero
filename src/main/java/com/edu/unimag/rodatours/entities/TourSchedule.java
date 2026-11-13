package com.edu.unimag.rodatours.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(
    name = "tour_schedules",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_tour_schedules_salida",
            columnNames = {"tour_id", "fecha_tour", "hora_inicio"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TourSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotNull(message = "El tour asociado es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guia_id")
    private Guide guia;

    @NotNull(message = "La fecha del tour es obligatoria")
    @Column(name = "fecha_tour", nullable = false)
    @ToString.Include
    private LocalDate fechaTour;

    @NotNull(message = "La hora de inicio es obligatoria")
    @Column(name = "hora_inicio", nullable = false)
    @ToString.Include
    private LocalTime horaInicio;

    @NotNull(message = "El total de cupos es obligatorio")
    @Min(value = 1, message = "El cupo total debe ser al menos 1")
    @Column(name = "cupos_totales", nullable = false)
    @ToString.Include
    private Integer cuposTotales;

    @NotNull(message = "Los cupos disponibles son obligatorios")
    @Min(value = 0, message = "Los cupos disponibles no pueden ser negativos")
    @Column(name = "cupos_disponibles", nullable = false)
    @ToString.Include
    private Integer cuposDisponibles;

    @OneToMany(mappedBy = "horario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reservation> reservas = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.cuposDisponibles == null && this.cuposTotales != null) {
            this.cuposDisponibles = this.cuposTotales;
        }
    }


    public LocalDateTime getStartDateTime() {
        if (this.fechaTour == null || this.horaInicio == null) {
            return null;
        }
        return LocalDateTime.of(this.fechaTour, this.horaInicio);
    }


    public LocalDateTime calculateEndDateTime() {
        LocalDateTime start = getStartDateTime();
        if (start == null || this.tour == null || this.tour.getDuracionHoras() == null) {
            return null;
        }
        long minutes = (long) (this.tour.getDuracionHoras().doubleValue() * 60);
        return start.plusMinutes(minutes);
    }

    public boolean hasAvailability(int personas) {
        return this.cuposDisponibles != null && this.cuposDisponibles >= personas;
    }

    public void discountSpots(int personas) {
        if (personas <= 0) {
            throw new IllegalArgumentException("La cantidad de personas a descontar debe ser mayor a cero.");
        }
        if (!hasAvailability(personas)) {
            throw new IllegalStateException("No hay suficientes cupos disponibles para cubrir " + personas + " personas.");
        }
        this.cuposDisponibles -= personas;
    }


    public void releaseSpots(int personas) {
        if (personas <= 0) {
            throw new IllegalArgumentException("La cantidad de personas a liberar debe ser mayor a cero.");
        }
        int nuevoDisponible = (this.cuposDisponibles == null ? 0 : this.cuposDisponibles) + personas;
        if (this.cuposTotales != null && nuevoDisponible > this.cuposTotales) {
            this.cuposDisponibles = this.cuposTotales;
        } else {
            this.cuposDisponibles = nuevoDisponible;
        }
    }

    public void addReservation(Reservation reservation) {
        if (reservation != null) {
            this.reservas.add(reservation);
            reservation.setHorario(this);
        }
    }

    public void removeReservation(Reservation reservation) {
        if (reservation != null) {
            this.reservas.remove(reservation);
            reservation.setHorario(null);
        }
    }
}
