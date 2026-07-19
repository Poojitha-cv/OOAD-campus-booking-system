package com.campus.booking.service;

import com.campus.booking.model.Booking;
import com.campus.booking.model.Resource;
import com.campus.booking.model.User;
import com.campus.booking.repository.BookingRepository;
import com.campus.booking.repository.ResourceRepository;
import com.campus.booking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GRASP: High Cohesion
 * ReportService has one focused responsibility: generating admin reports.
 * It does NOT handle booking logic, conflict detection, or notifications.
 * All report-related data gathering is centralized here.
 */
@Service
public class ReportService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    // Total bookings count
    public long getTotalBookings() {
        return bookingRepository.count();
    }

    // Count by status
    public long countByStatus(String status) {
        return bookingRepository.findAll().stream()
                .filter(b -> status.equalsIgnoreCase(b.getStatus()))
                .count();
    }

    // All bookings (for admin table)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    // All conflict bookings
    public List<Booking> getConflictBookings() {
        return bookingRepository.findAll().stream()
                .filter(b -> "CONFLICT".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());
    }

    // Most booked resource
    public String getMostBookedResource() {
        return bookingRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        b -> b.getResource().getName(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }

    // All resources
    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    // All users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Bookings per resource (for report table)
    public Map<String, Long> getBookingsPerResource() {
        return bookingRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        b -> b.getResource().getName(), Collectors.counting()));
    }
    
}