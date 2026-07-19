package com.campus.booking.strategy;

import com.campus.booking.model.Booking;
import com.campus.booking.model.Resource;
import com.campus.booking.model.WaitlistEntry;
import com.campus.booking.repository.BookingRepository;
import com.campus.booking.repository.ResourceRepository;

import java.util.List;

/**
 * Strategy 3: Alternate Room
 *
 * Behavior:
 * 1. Go through every waitlisted entry (or the conflict booking itself)
 * 2. Find a different resource of the same type that is free for that time slot
 * 3. If found → reassign the booking to the alternate resource, SAVE it, return entry
 * 4. If none found → return null
 */
public class AlternateRoomResolutionStrategy implements ConflictResolutionStrategy {

    private final ResourceRepository resourceRepository;
    private final BookingRepository bookingRepository;
    private final Resource originalResource;

    private String suggestedAlternate = null;

    public AlternateRoomResolutionStrategy(ResourceRepository resourceRepository,
                                           BookingRepository bookingRepository,
                                           Resource originalResource) {
        this.resourceRepository = resourceRepository;
        this.bookingRepository = bookingRepository;
        this.originalResource = originalResource;
    }

    @Override
    public WaitlistEntry selectCandidate(List<WaitlistEntry> queue) {

        if (queue == null || queue.isEmpty()) {
            return null;
        }

        for (WaitlistEntry entry : queue) {

            List<Resource> sameTypeResources =
                    resourceRepository.findByType(originalResource.getType());

            for (Resource alt : sameTypeResources) {

                // Skip the original resource — we need a different one
                if (alt.getId().equals(originalResource.getId())) continue;

                // Check if this alternate resource is free during the entry's time slot
                List<Booking> confirmedBookings =
                        bookingRepository.findByResourceIdAndStatus(alt.getId(), "CONFIRMED");

                boolean isFree = confirmedBookings.stream().noneMatch(b ->
                        entry.getStartTime().isBefore(b.getEndTime()) &&
                        entry.getEndTime().isAfter(b.getStartTime())
                );

                if (isFree) {
                    // Reassign booking to alternate resource and SAVE to database
                    Booking booking = entry.getBooking();
                    booking.setResource(alt);
                    bookingRepository.save(booking); // critical — persists the reassignment

                    suggestedAlternate = alt.getName();
                    return entry;
                }
            }
        }

        return null;
    }

    public String getSuggestedAlternate() {
        return suggestedAlternate;
    }
}