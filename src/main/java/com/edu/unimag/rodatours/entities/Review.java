package com.edu.unimag.rodatours.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "resenas",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_resena_reserva", columnNames = "reservacion_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotNull(message = "La reserva asociada a la reseña es obligatoria")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservacion_id", nullable = false, unique = true)
    private Reservation reserva;

    @NotNull(message = "La calificación es obligatoria")
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    @Column(name = "calificacion", nullable = false)
    @ToString.Include
    private Integer calificacion;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
    }
}
