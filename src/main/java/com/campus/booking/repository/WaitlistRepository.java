package com.campus.booking.repository;

import com.campus.booking.model.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {
    List<WaitlistEntry> findByResourceIdOrderByAddedAtAsc(Long resourceId);
    List<WaitlistEntry> findByUserIdOrderByAddedAtDesc(Long userId);
    Optional<WaitlistEntry> findByBookingId(Long bookingId);
}