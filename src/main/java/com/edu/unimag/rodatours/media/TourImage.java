package com.edu.unimag.rodatours.media;

import com.edu.unimag.rodatours.domain.entities.Tour;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name="tour_images") @Getter @Setter
public class TourImage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tour_id",nullable=false) private Tour tour;
    @Column(nullable=false,unique=true,length=150) private String publicId;
    @Column(nullable=false,length=15) private String status="PENDING";
    @Column(length=1500) private String url;
    @Column(nullable=false) private Instant expiresAt;
    @Version private Long version;
}
