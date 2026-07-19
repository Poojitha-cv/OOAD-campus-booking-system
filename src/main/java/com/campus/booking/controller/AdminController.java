package com.campus.booking.controller;

import com.campus.booking.decorator.RecurringBookingDecorator;
import com.campus.booking.model.Booking;
import com.campus.booking.model.BookingRequest;
import com.campus.booking.model.Resource;
import com.campus.booking.repository.BookingRepository;
import com.campus.booking.service.ReportService;
import com.campus.booking.service.ResourceService;
import com.campus.booking.service.WaitlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GRASP: High Cohesion — delegates all report data to ReportService.
 * Handles: Admin Dashboard, Resource CRUD, Reports,
 *          Winner/Loser outcome, Admin Arbitration (force-resolve),
 *          Recurring Booking (via Decorator pattern).
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private WaitlistService waitlistService;

    @Autowired
    private RecurringBookingDecorator recurringBookingDecorator;

    // ── Security check helper ──────────────────────────────────────────────
    private boolean isAdmin(HttpSession session) {
        return "ADMIN".equalsIgnoreCase((String) session.getAttribute("role"))
                && session.getAttribute("userId") != null;
    }

    // ── Dashboard ──────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/";

        model.addAttribute("totalBookings",    reportService.getTotalBookings());
        model.addAttribute("confirmedCount",   reportService.countByStatus("CONFIRMED"));
        model.addAttribute("conflictCount",    reportService.countByStatus("CONFLICT"));
        model.addAttribute("cancelledCount",   reportService.countByStatus("CANCELLED"));
        model.addAttribute("mostBooked",       reportService.getMostBookedResource());
        model.addAttribute("resources",        reportService.getAllResources());
        model.addAttribute("users",            reportService.getAllUsers());
        model.addAttribute("bookingsPerRes",   reportService.getBookingsPerResource());
        model.addAttribute("allBookings",      reportService.getAllBookings());
        model.addAttribute("conflictBookings", reportService.getConflictBookings());
        return "admin-dashboard";
    }

    // ── Resource CRUD ──────────────────────────────────────────────────────

    @PostMapping("/resource/add")
    public String addResource(@RequestParam String name,
                              @RequestParam String type,
                              HttpSession session) {
        if (!isAdmin(session)) return "redirect:/";
        resourceService.addResource(name, type);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/resource/delete/{id}")
    public String deleteResource(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/";
        resourceService.deleteResource(id);
        return "redirect:/admin/dashboard";
    }

    // ── Reports ────────────────────────────────────────────────────────────

    @GetMapping("/reports")
    public String reports(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/";

        model.addAttribute("allBookings",      reportService.getAllBookings());
        model.addAttribute("conflictBookings", reportService.getConflictBookings());
        model.addAttribute("bookingsPerRes",   reportService.getBookingsPerResource());
        model.addAttribute("totalBookings",    reportService.getTotalBookings());
        model.addAttribute("confirmedCount",   reportService.countByStatus("CONFIRMED"));
        model.addAttribute("conflictCount",    reportService.countByStatus("CONFLICT"));
        model.addAttribute("cancelledCount",   reportService.countByStatus("CANCELLED"));
        return "admin-reports";
    }

    // ── Admin Arbitration Screen (force-resolve conflicts) ─────────────────

    @GetMapping("/conflicts")
    public String conflictsScreen(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/";

        List<Booking> conflicts = reportService.getConflictBookings();
        model.addAttribute("conflictBookings", conflicts);
        return "admin-arbitration";
    }

    /**
     * Force-resolve: admin picks winner/loser manually.
     * winnerId   = booking to CONFIRM
     * loserId    = booking to CANCEL
     */
    @PostMapping("/conflicts/resolve")
    public String forceResolve(@RequestParam Long winnerId,
                               @RequestParam Long loserId,
                               @RequestParam(defaultValue = "FCFS") String strategy,
                               Model model,
                               HttpSession session) {
        if (!isAdmin(session)) return "redirect:/";

        Booking winner = bookingRepository.findById(winnerId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        Booking loser = bookingRepository.findById(loserId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        winner.setStatus("CONFIRMED");
        bookingRepository.save(winner);

        // Use WaitlistService to cancel loser + auto-promote next in queue
        String promoteResult = waitlistService.resolveWithStrategy(loserId, strategy);

        model.addAttribute("winner", winner);
        model.addAttribute("loser", loser);
        model.addAttribute("promoteResult", promoteResult);
        model.addAttribute("strategy", strategy);
        return "admin-arbitration-result";
    }

    // ── Recurring Booking (Decorator Pattern) ──────────────────────────────

    @GetMapping("/recurring")
    public String recurringForm(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/";
        model.addAttribute("resources", reportService.getAllResources());
        return "admin-recurring";
    }

    @PostMapping("/recurring/create")
    public String createRecurring(
            @RequestParam Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam String purpose,
            @RequestParam int weeks,
            Model model,
            HttpSession session) {

        if (!isAdmin(session)) return "redirect:/";

        Long userId = (Long) session.getAttribute("userId");

        BookingRequest base = new BookingRequest();
        base.setResourceId(resourceId);
        base.setUserId(userId);
        base.setStartTime(startTime);
        base.setEndTime(endTime);
        base.setPurpose(purpose);

        List<String> results = recurringBookingDecorator.createRecurringBookings(base, weeks);

        model.addAttribute("results", results);
        model.addAttribute("weeks", weeks);
        return "admin-recurring-result";
    }
}