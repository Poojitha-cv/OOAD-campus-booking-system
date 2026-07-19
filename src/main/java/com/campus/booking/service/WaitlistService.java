package com.campus.booking.service;

import com.campus.booking.model.*;
import com.campus.booking.observer.BookingEventObserver;
import com.campus.booking.repository.*;
import com.campus.booking.strategy.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GRASP: Low Coupling
 * WaitlistService depends ONLY on repositories and the observer interface.
 *
 * DESIGN PATTERN: Observer (Subject)
 * Fires BookingEventObserver on cancel, promote, conflict events.
 *
 * DESIGN PATTERN: Strategy
 * Delegates resolution logic to ConflictResolutionStrategy implementations.
 * Pragna's Major 2 — Resolution Strategies (FCFS, Priority, Alternate Room)
 */
@Service
public class WaitlistService {

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private BookingEventObserver notificationObserver;

    // ──────────────────────────────────────────────
    // ADD TO WAITLIST
    // ──────────────────────────────────────────────

    public WaitlistEntry addToWaitlist(Booking conflictBooking) {
        List<WaitlistEntry> existing =
                waitlistRepository.findByResourceIdOrderByAddedAtAsc(
                        conflictBooking.getResource().getId());

        WaitlistEntry entry = new WaitlistEntry();
        entry.setBooking(conflictBooking);
        entry.setResource(conflictBooking.getResource());
        entry.setUser(conflictBooking.getUser());
        entry.setStartTime(conflictBooking.getStartTime());
        entry.setEndTime(conflictBooking.getEndTime());
        entry.setPurpose(conflictBooking.getPurpose());
        entry.setPosition(existing.size() + 1);

        notificationObserver.onBookingConflict(conflictBooking);
        return waitlistRepository.save(entry);
    }

    // ──────────────────────────────────────────────
    // CANCEL A CONFIRMED BOOKING → auto-promote next in waitlist (FCFS)
    // Called when the CONFIRMED booking holder clicks "Cancel Booking"
    // ──────────────────────────────────────────────

    @Transactional
    public String cancelAndPromote(Long bookingId) {
        return autoPromoteAfterCancel(bookingId);
    }

    // ──────────────────────────────────────────────
    // AUTO-PROMOTE after a CONFIRMED booking is cancelled
    // Finds earliest CONFLICT booking on that slot and promotes it
    // ──────────────────────────────────────────────

    @Transactional
    public String autoPromoteAfterCancel(Long cancelledBookingId) {
        Booking cancelled = bookingRepository.findById(cancelledBookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        cancelled.setStatus("CANCELLED");
        bookingRepository.save(cancelled);
        notificationObserver.onBookingCancelled(cancelled);

        Long resourceId = cancelled.getResource().getId();

        // Find the earliest CONFLICT booking overlapping the freed slot
        List<Booking> conflictBookings =
                bookingRepository.findByResourceIdAndStatus(resourceId, "CONFLICT");

        Booking toPromote = conflictBookings.stream()
                .filter(b ->
                        b.getStartTime().isBefore(cancelled.getEndTime()) &&
                        b.getEndTime().isAfter(cancelled.getStartTime()))
                .min(Comparator.comparing(Booking::getStartTime))
                .orElse(null);

        if (toPromote == null) {
            reorderQueue(resourceId);
            return "Booking cancelled. No waitlisted entries to promote.";
        }

        toPromote.setStatus("CONFIRMED");
        bookingRepository.save(toPromote);
        removeFromWaitlist(toPromote.getId());
        reorderQueue(resourceId);
        notificationObserver.onBookingPromoted(toPromote);

        return "Booking cancelled. [FCFS] "
                + toPromote.getUser().getName()
                + "'s waitlisted booking has been promoted!";
    }

    // ──────────────────────────────────────────────
    // STRATEGY DISPATCHER
    // bookingId = the CONFLICT booking of the person clicking "Resolve Conflict"
    // strategy: "FCFS" | "PRIORITY" | "ALTERNATE"
    // ──────────────────────────────────────────────

    @Transactional
    public String resolveWithStrategy(Long bookingId, String strategy) {

        // This is the CONFLICT booking — the person who wants to resolve their conflict
        Booking conflictBooking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        Long resourceId = conflictBooking.getResource().getId();

        // Find the CONFIRMED booking that is currently blocking this slot
        Booking confirmedBooking = bookingRepository
                .findByResourceIdAndStatus(resourceId, "CONFIRMED")
                .stream()
                .filter(b ->
                        conflictBooking.getStartTime().isBefore(b.getEndTime()) &&
                        conflictBooking.getEndTime().isAfter(b.getStartTime()))
                .findFirst()
                .orElse(null);

        // ── PRIORITY strategy ────────────────────────────────────────────────
        if ("PRIORITY".equalsIgnoreCase(strategy)) {

            if (confirmedBooking == null) {
                // No one blocking the slot — confirm directly
                conflictBooking.setStatus("CONFIRMED");
                bookingRepository.save(conflictBooking);
                removeFromWaitlist(bookingId);
                return "[PRIORITY] Slot was already free. Your booking has been confirmed!";
            }

            int resolverPriority = rolePriority(conflictBooking.getUser().getRole());
            int holderPriority   = rolePriority(confirmedBooking.getUser().getRole());

            if (resolverPriority < holderPriority) {
                // Resolver has HIGHER role (lower number) → cancel holder, confirm resolver
                confirmedBooking.setStatus("CANCELLED");
                bookingRepository.save(confirmedBooking);
                notificationObserver.onBookingCancelled(confirmedBooking);

                conflictBooking.setStatus("CONFIRMED");
                bookingRepository.save(conflictBooking);
                removeFromWaitlist(bookingId);
                notificationObserver.onBookingPromoted(conflictBooking);

                return "[PRIORITY] Your booking has been confirmed! "
                        + confirmedBooking.getUser().getName()
                        + " (" + confirmedBooking.getUser().getRole() + ")"
                        + "'s booking was cancelled because you ("
                        + conflictBooking.getUser().getRole()
                        + ") have higher priority.";

            } else {
                // Resolver does NOT beat the current holder — no change
                return "[PRIORITY] No change. "
                        + confirmedBooking.getUser().getName()
                        + " (" + confirmedBooking.getUser().getRole() + ")"
                        + " has equal or higher priority than you ("
                        + conflictBooking.getUser().getRole()
                        + "). Your booking remains on the waitlist.";
            }
        }

        // ── ALTERNATE strategy ───────────────────────────────────────────────
        if ("ALTERNATE".equalsIgnoreCase(strategy)) {

            AlternateRoomResolutionStrategy altStrategy =
                    new AlternateRoomResolutionStrategy(
                            resourceRepository, bookingRepository,
                            conflictBooking.getResource());

            // Wrap the conflict booking as a WaitlistEntry for the strategy interface
            WaitlistEntry selfEntry = new WaitlistEntry();
            selfEntry.setBooking(conflictBooking);
            selfEntry.setUser(conflictBooking.getUser());
            selfEntry.setStartTime(conflictBooking.getStartTime());
            selfEntry.setEndTime(conflictBooking.getEndTime());

            WaitlistEntry chosen = altStrategy.selectCandidate(List.of(selfEntry));

            if (chosen != null) {
                String altName = altStrategy.getSuggestedAlternate();
                // conflictBooking resource was already updated inside altStrategy + saved
                conflictBooking.setStatus("CONFIRMED");
                bookingRepository.save(conflictBooking);
                removeFromWaitlist(bookingId);
                notificationObserver.onBookingPromoted(conflictBooking);

                return "[ALTERNATE] Your booking has been moved to \""
                        + altName + "\" and confirmed!";
            }

            return "[ALTERNATE] No alternate resource of the same type is available "
                    + "for your time slot. Your booking remains on the waitlist.";
        }

        // ── FCFS strategy ────────────────────────────────────────────────────
        // FCFS from the resolver's perspective: confirm them if slot is now free,
        // otherwise tell them they are already queued and will be auto-promoted.
        if (confirmedBooking == null) {
            conflictBooking.setStatus("CONFIRMED");
            bookingRepository.save(conflictBooking);
            removeFromWaitlist(bookingId);
            return "[FCFS] Slot is now free. Your booking has been confirmed!";
        }

        return "[FCFS] The slot is still held by "
                + confirmedBooking.getUser().getName()
                + " (" + confirmedBooking.getUser().getRole() + "). "
                + "You are on the waitlist and will be automatically promoted when they cancel.";
    }

    // ──────────────────────────────────────────────
    // QUERIES
    // ──────────────────────────────────────────────

    public List<WaitlistEntry> getWaitlistForResource(Long resourceId) {
        return waitlistRepository.findByResourceIdOrderByAddedAtAsc(resourceId);
    }

    public List<WaitlistEntry> getWaitlistForUser(Long userId) {
        return waitlistRepository.findByUserIdOrderByAddedAtDesc(userId);
    }

    // ──────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────

    private void removeFromWaitlist(Long bookingId) {
        waitlistRepository.findByBookingId(bookingId)
                .ifPresent(waitlistRepository::delete);
    }

    private void reorderQueue(Long resourceId) {
        List<WaitlistEntry> queue =
                waitlistRepository.findByResourceIdOrderByAddedAtAsc(resourceId);
        for (int i = 0; i < queue.size(); i++) {
            queue.get(i).setPosition(i + 1);
            waitlistRepository.save(queue.get(i));
        }
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