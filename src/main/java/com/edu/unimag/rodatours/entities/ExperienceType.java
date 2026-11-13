package com.edu.unimag.rodatours.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(
    name = "experience_types",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tipo_experiencia_nombre", columnNames = "nombre")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ExperienceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    @ToString.Include
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @OneToMany(mappedBy = "tipoExperiencia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Tour> tours = new ArrayList<>();

    public void addTour(Tour tour) {
        if (tour != null) {
            this.tours.add(tour);
            tour.setTipoExperiencia(this);
        }
    }

    public void removeTour(Tour tour) {
        if (tour != null) {
            this.tours.remove(tour);
            tour.setTipoExperiencia(null);
        }
    }
}
