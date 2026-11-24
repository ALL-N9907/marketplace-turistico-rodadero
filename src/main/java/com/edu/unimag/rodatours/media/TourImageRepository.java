package com.edu.unimag.rodatours.media;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface TourImageRepository extends JpaRepository<TourImage,Long> {
    long countByTourId(Long id);
    List<TourImage> findByTourIdOrderByIdAsc(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from TourImage i where i.id = :id and i.tour.id = :tourId")
    Optional<TourImage> lock(@Param("tourId") Long tourId,@Param("id") Long id);
}
