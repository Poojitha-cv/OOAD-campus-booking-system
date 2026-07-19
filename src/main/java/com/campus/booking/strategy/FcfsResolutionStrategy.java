package com.campus.booking.strategy;

import com.campus.booking.model.WaitlistEntry;
import java.util.List;

/**
 * Strategy 1: First Come First Served
 * Picks the earliest entry in the queue (already sorted by addedAt asc).
 */
public class FcfsResolutionStrategy implements ConflictResolutionStrategy {

    @Override
    public WaitlistEntry selectCandidate(List<WaitlistEntry> queue) {
        return queue.isEmpty() ? null : queue.get(0);
    }
}