package com.campus.booking.repository;

import com.campus.booking.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    // Check if user already rated a booking (prevent duplicate ratings)
    Optional<Rating> findByBookingId(Long bookingId);

    // All ratings for a resource (to show on resource page)
    List<Rating> findByResourceId(Long resourceId);

    // All ratings submitted by a user
    List<Rating> findByUserId(Long userId);

    // Average stars for a resource
    @Query("SELECT AVG(r.stars) FROM Rating r WHERE r.resource.id = :resourceId")
    Double findAverageStarsByResourceId(Long resourceId);
}