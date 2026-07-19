package com.campus.booking.controller;

import com.campus.booking.model.WaitlistEntry;
import com.campus.booking.service.WaitlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MVC Controller — Cancellation, Waitlist, Resolution Strategy UI
 * Pragna's Major 3 + Major 2 (Resolution Strategies)
 */
@Controller
@RequestMapping("/waitlist")
public class WaitlistController {

    @Autowired
    private WaitlistService waitlistService;

    /** Show the user's waitlist entries */
    @GetMapping("/my")
    public String myWaitlist(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/";

        List<WaitlistEntry> entries = waitlistService.getWaitlistForUser(userId);
        model.addAttribute("entries", entries);
        return "waitlist";
    }

    /**
     * Cancel a CONFIRMED booking — auto-promotes earliest CONFLICT booking (FCFS).
     * Called when the CONFIRMED booking HOLDER clicks "Cancel Booking".
     */
    @PostMapping("/cancel/{bookingId}")
    public String cancelBooking(@PathVariable Long bookingId,
                                Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/";

        String result = waitlistService.autoPromoteAfterCancel(bookingId);
        model.addAttribute("message", result);
        model.addAttribute("strategy", "FCFS");
        return "cancel-result";
    }

    /**
     * Resolve a CONFLICT booking using a chosen strategy.
     * Called when the CONFLICT booking OWNER picks a strategy from the resolve screen.
     * strategy param: FCFS | PRIORITY | ALTERNATE
     */
    @PostMapping("/resolve/{bookingId}")
    public String resolveWithStrategy(@PathVariable Long bookingId,
                                      @RequestParam(defaultValue = "FCFS") String strategy,
                                      Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/";

        String result = waitlistService.resolveWithStrategy(bookingId, strategy);
        model.addAttribute("message", result);
        model.addAttribute("strategy", strategy);
        return "cancel-result";
    }

    /**
     * Show the resolution strategy picker for a CONFLICT booking.
     * Linked from "Resolve Conflict" button on my-bookings page.
     */
    @GetMapping("/resolve-screen/{bookingId}")
    public String resolveScreen(@PathVariable Long bookingId,
                                Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/";

        model.addAttribute("bookingId", bookingId);
        return "resolve-strategy";
    }
}