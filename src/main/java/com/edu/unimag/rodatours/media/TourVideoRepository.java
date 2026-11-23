package com.edu.unimag.rodatours.media;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface TourVideoRepository extends JpaRepository<TourVideo, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from TourVideo v where v.tourId = :id")
    Optional<TourVideo> findByIdForUpdate(Long id);
}
