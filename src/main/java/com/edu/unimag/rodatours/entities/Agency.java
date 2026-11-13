package com.edu.unimag.rodatours.entities;

import com.edu.unimag.rodatours.entities.enums.AgencyStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Empresa operadora que diseña, publica y opera tours y experiencias.
 */
@Entity
@Table(
    name = "agencies",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_agencia_nit_registro", columnNames = "nit_o_registro")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Agency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotBlank(message = "El nombre comercial de la agencia es obligatorio")
    @Size(max = 150, message = "El nombre comercial no puede superar los 150 caracteres")
    @Column(name = "nombre_comercial", nullable = false, length = 150)
    @ToString.Include
    private String nombreComercial;

    @NotBlank(message = "El NIT o registro legal es obligatorio")
    @Size(max = 30, message = "El NIT o registro no puede superar los 30 caracteres")
    @Column(name = "nit_o_registro", nullable = false, unique = true, length = 30)
    @ToString.Include
    private String nitORegistro;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @NotBlank(message = "La dirección de la agencia es obligatoria")
    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    @Column(name = "direccion", nullable = false, length = 255)
    private String direccion;

    @NotBlank(message = "El teléfono de contacto es obligatorio")
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    @Column(name = "telefono_contacto", nullable = false, length = 20)
    private String telefonoContacto;

    @NotNull(message = "El estado de la agencia es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    @ToString.Include
    private AgencyStatus estado = AgencyStatus.ACTIVE;

    @OneToMany(mappedBy = "agencia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Tour> tours = new ArrayList<>();

    @OneToMany(mappedBy = "agencia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Guide> guias = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.estado == null) {
            this.estado = AgencyStatus.ACTIVE;
        }
    }

    public boolean isActive() {
        return this.estado == AgencyStatus.ACTIVE;
    }

    public void addTour(Tour tour) {
        if (tour != null) {
            this.tours.add(tour);
            tour.setAgencia(this);
        }
    }

    public void removeTour(Tour tour) {
        if (tour != null) {
            this.tours.remove(tour);
            tour.setAgencia(null);
        }
    }

    public void addGuide(Guide guide) {
        if (guide != null) {
            this.guias.add(guide);
            guide.setAgencia(this);
        }
    }

    public void removeGuide(Guide guide) {
        if (guide != null) {
            this.guias.remove(guide);
            guide.setAgencia(null);
        }
    }
}
