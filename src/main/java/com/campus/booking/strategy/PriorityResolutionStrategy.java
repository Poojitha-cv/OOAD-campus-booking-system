package com.campus.booking.strategy;

import com.campus.booking.model.WaitlistEntry;
import java.util.Comparator;
import java.util.List;

/**
 * Strategy 2: Priority-based — Faculty > Admin > Student
 * Among all eligible queue entries, picks the one with highest role priority.
 */
public class PriorityResolutionStrategy implements ConflictResolutionStrategy {

    @Override
    public WaitlistEntry selectCandidate(List<WaitlistEntry> queue) {
        return queue.stream()
                .min(Comparator.comparingInt(e -> rolePriority(e.getUser().getRole())))
                .orElse(null);
    }

    private int rolePriority(String role) {
        if (role == null) return 99;
        switch (role.toUpperCase()) {
            case "FACULTY": return 1;
            case "ADMIN":   return 2;
            case "STUDENT": return 3;
            default:        return 99;
        }
    }
}