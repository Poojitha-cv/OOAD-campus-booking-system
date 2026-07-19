package com.campus.booking.strategy;

import com.campus.booking.model.WaitlistEntry;
import java.util.List;

/**
 * DESIGN PATTERN: Strategy
 * Defines the contract for all conflict resolution strategies.
 * Pragna's Major 2 — Resolution Strategies
 */
public interface ConflictResolutionStrategy {
    /**
     * Pick the best candidate from the waitlist queue to promote.
     * Returns null if no eligible candidate found.
     */
    WaitlistEntry selectCandidate(List<WaitlistEntry> queue);
}