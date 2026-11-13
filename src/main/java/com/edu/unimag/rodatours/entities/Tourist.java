package com.edu.unimag.rodatours.entities;

import com.edu.unimag.rodatours.entities.enums.TouristStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(
    name = "turistas",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_turista_correo", columnNames = "correo_electronico")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Tourist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres")
    @Column(name = "nombre_completo", nullable = false, length = 150)
    @ToString.Include
    private String nombreCompleto;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe proporcionar un formato de correo electrónico válido")
    @Size(max = 255, message = "El correo electrónico no puede superar los 255 caracteres")
    @Column(name = "correo_electronico", nullable = false, unique = true, length = 255)
    @ToString.Include
    private String correoElectronico;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 255, message = "La contraseña no puede superar los 255 caracteres")
    @Column(name = "contrasena", nullable = false, length = 255)
    private String contrasena;

    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    @Column(name = "telefono", length = 20)
    private String telefono;

    @NotNull(message = "El estado del turista es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    @ToString.Include
    private TouristStatus estado = TouristStatus.ACTIVE;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "turista", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reservation> reservas = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = TouristStatus.ACTIVE;
        }
    }

    public boolean isActive() {
        return this.estado == TouristStatus.ACTIVE;
    }

    public void addReservation(Reservation reservation) {
        if (reservation != null) {
            this.reservas.add(reservation);
            reservation.setTurista(this);
        }
    }

    public void removeReservation(Reservation reservation) {
        if (reservation != null) {
            this.reservas.remove(reservation);
            reservation.setTurista(null);
        }
    }
}
