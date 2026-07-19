package com.campus.booking.controller;

import com.campus.booking.model.*;
import com.campus.booking.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Minor 3 — Rating & Feedback
 * Allows users to rate a resource after a CANCELLED or CONFIRMED booking ends.
 * GRASP: Low Coupling — RatingController only depends on repositories, not services.
 */
@Controller
@RequestMapping("/rating")
public class RatingController {

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    /**
     * Show the rating form for a specific booking.
     * Only accessible if booking belongs to logged-in user and is CONFIRMED or CANCELLED.
     */
    @GetMapping("/form/{bookingId}")
    public String showRatingForm(@PathVariable Long bookingId,
                                  Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/";

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Security: only the owner can rate
        if (!booking.getUser().getId().equals(userId)) return "redirect:/booking/my";

        // Only rate CONFIRMED or CANCELLED bookings (not PENDING/CONFLICT)
        if (!booking.getStatus().equals("CONFIRMED") && !booking.getStatus().equals("CANCELLED")) {
            return "redirect:/booking/my";
        }

        // Already rated?
        boolean alreadyRated = ratingRepository.findByBookingId(bookingId).isPresent();
        if (alreadyRated) {
            model.addAttribute("message", "You have already rated this booking.");
            return "rating-done";
        }

        model.addAttribute("booking", booking);
        return "rating-form";
    }

    /**
     * Submit a rating for a booking.
     */
    @PostMapping("/submit")
    public String submitRating(@RequestParam Long bookingId,
                                @RequestParam int stars,
                                @RequestParam(required = false) String comment,
                                HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/";

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getId().equals(userId)) return "redirect:/booking/my";

        // Prevent duplicate rating
        if (ratingRepository.findByBookingId(bookingId).isPresent()) {
            model.addAttribute("message", "You have already rated this booking.");
            return "rating-done";
        }

        // Validate stars
        if (stars < 1 || stars > 5) stars = 3;

        Rating rating = new Rating();
        rating.setBooking(booking);
        rating.setUser(booking.getUser());
        rating.setResource(booking.getResource());
        rating.setStars(stars);
        rating.setComment(comment != null ? comment.trim() : "");
        ratingRepository.save(rating);

        model.addAttribute("message", "Thank you for your feedback!");
        return "rating-done";
    }

    /**
     * Show all ratings for a resource (public view).
     */
    @GetMapping("/resource/{resourceId}")
    public String resourceRatings(@PathVariable Long resourceId, Model model) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        List<Rating> ratings = ratingRepository.findByResourceId(resourceId);
        Double avg = ratingRepository.findAverageStarsByResourceId(resourceId);

        model.addAttribute("resource", resource);
        model.addAttribute("ratings", ratings);
        model.addAttribute("avgStars", avg != null ? String.format("%.1f", avg) : "No ratings yet");
        return "resource-ratings";
    }
}