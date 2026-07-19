package com.campus.booking.repository;

import com.campus.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByResourceId(Long resourceId);

    // Use this for conflict detection — only CONFIRMED bookings block slots
    List<Booking> findByResourceIdAndStatus(Long resourceId, String status);
    List<Booking> findByUserId(Long userId); 
}