package com.edu.unimag.rodatours.entities;

import com.edu.unimag.rodatours.entities.enums.DifficultyLevel;
import com.edu.unimag.rodatours.entities.enums.TourStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "tours")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotNull(message = "La agencia que ofrece el tour es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agencia_id", nullable = false)
    private Agency agencia;

    @NotNull(message = "El tipo de experiencia es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_experiencia_id", nullable = false)
    private ExperienceType tipoExperiencia;

    @NotBlank(message = "El título del tour es obligatorio")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    @Column(name = "titulo", nullable = false, length = 150)
    @ToString.Include
    private String titulo;

    @NotBlank(message = "La descripción del tour es obligatoria")
    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @NotBlank(message = "El destino del tour es obligatorio")
    @Size(max = 120, message = "El destino no puede superar los 120 caracteres")
    @Column(name = "destino", nullable = false, length = 120)
    @ToString.Include
    private String destino;

    @NotNull(message = "El precio por persona es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    @ToString.Include
    private BigDecimal precio;

    @NotNull(message = "La duración en horas es obligatoria")
    @DecimalMin(value = "0.1", message = "La duración en horas debe ser mayor que cero")
    @Column(name = "duracion_horas", nullable = false, precision = 4, scale = 1)
    private BigDecimal duracionHoras;

    @NotNull(message = "El nivel de dificultad es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_dificultad", nullable = false, length = 10)
    @ToString.Include
    private DifficultyLevel nivelDificultad;

    @Column(name = "servicios_incluidos", columnDefinition = "TEXT")
    private String serviciosIncluidos;

    @NotNull(message = "El estado del tour es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    @ToString.Include
    private TourStatus estado = TourStatus.ACTIVE;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TourSchedule> horarios = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.estado == null) {
            this.estado = TourStatus.ACTIVE;
        }
    }

    public boolean isActive() {
        return this.estado == TourStatus.ACTIVE;
    }

    public void addSchedule(TourSchedule schedule) {
        if (schedule != null) {
            this.horarios.add(schedule);
            schedule.setTour(this);
        }
    }

    public void removeSchedule(TourSchedule schedule) {
        if (schedule != null) {
            this.horarios.remove(schedule);
            schedule.setTour(null);
        }
    }
}
