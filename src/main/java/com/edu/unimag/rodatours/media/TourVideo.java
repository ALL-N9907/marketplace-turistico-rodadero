package com.edu.unimag.rodatours.media;

import com.edu.unimag.rodatours.domain.entities.Tour;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tour_videos")
@Getter
@Setter
@NoArgsConstructor
public class TourVideo {
    @Id
    private Long tourId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @Column(nullable = false, unique = true)
    private String publicId;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private boolean ready;

    @Column(length = 1500)
    private String secureUrl;
    private String format;
    private Long bytes;
    private Double durationSeconds;
    private Integer width;
    private Integer height;

    @Version
    private Long version;
}
