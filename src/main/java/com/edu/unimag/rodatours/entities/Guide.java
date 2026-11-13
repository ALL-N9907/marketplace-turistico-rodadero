package com.edu.unimag.rodatours.entities;

import com.edu.unimag.rodatours.entities.enums.GuideStatus;
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
    name = "guides",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_guia_documento", columnNames = "documento_identidad"),
        @UniqueConstraint(name = "uk_guia_registro", columnNames = "registro_guia"),
        @UniqueConstraint(name = "uk_guia_correo", columnNames = "correo_electronico")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Guide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotNull(message = "La agencia a la que pertenece el guía es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agencia_id", nullable = false)
    private Agency agencia;

    @NotBlank(message = "El nombre completo del guía es obligatorio")
    @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres")
    @Column(name = "nombre_completo", nullable = false, length = 150)
    @ToString.Include
    private String nombreCompleto;

    @NotBlank(message = "El documento de identidad es obligatorio")
    @Size(max = 30, message = "El documento no puede superar los 30 caracteres")
    @Column(name = "documento_identidad", nullable = false, unique = true, length = 30)
    @ToString.Include
    private String documentoIdentidad;

    @NotBlank(message = "El registro o tarjeta profesional es obligatorio")
    @Size(max = 30, message = "El registro no puede superar los 30 caracteres")
    @Column(name = "registro_guia", nullable = false, unique = true, length = 30)
    @ToString.Include
    private String registroGuia;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe proporcionar un formato de correo electrónico válido")
    @Size(max = 255, message = "El correo electrónico no puede superar los 255 caracteres")
    @Column(name = "correo_electronico", nullable = false, unique = true, length = 255)
    @ToString.Include
    private String correoElectronico;

    @NotBlank(message = "El teléfono del guía es obligatorio")
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Size(max = 100, message = "Los idiomas no pueden superar los 100 caracteres")
    @Column(name = "idiomas", length = 100)
    private String idiomas;

    @NotNull(message = "El estado del guía es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    @ToString.Include
    private GuideStatus estado = GuideStatus.ACTIVE;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "guia", fetch = FetchType.LAZY)
    @Builder.Default
    private List<TourSchedule> horarios = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = GuideStatus.ACTIVE;
        }
    }

    public boolean isActive() {
        return this.estado == GuideStatus.ACTIVE;
    }

    public void addSchedule(TourSchedule schedule) {
        if (schedule != null) {
            this.horarios.add(schedule);
            schedule.setGuia(this);
        }
    }

    public void removeSchedule(TourSchedule schedule) {
        if (schedule != null) {
            this.horarios.remove(schedule);
            schedule.setGuia(null);
        }
    }
}
