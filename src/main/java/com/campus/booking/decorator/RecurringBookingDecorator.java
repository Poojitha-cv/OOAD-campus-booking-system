package com.campus.booking.decorator;

import com.campus.booking.model.Booking;
import com.campus.booking.model.BookingRequest;
import com.campus.booking.service.BookingService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * DESIGN PATTERN: Decorator
 * Wraps BookingService to add recurring booking behaviour.
 * Instead of changing BookingService, this class decorates it —
 * calling createBooking() repeatedly for each weekly occurrence.
 */
@Component
public class RecurringBookingDecorator {

    private final BookingService bookingService;

    public RecurringBookingDecorator(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Creates 'weeks' number of weekly bookings starting from the request's time.
     * Each booking is shifted by 7 days from the previous one.
     */
    public List<String> createRecurringBookings(BookingRequest baseRequest, int weeks) {
        List<String> results = new ArrayList<>();

        for (int i = 0; i < weeks; i++) {
            BookingRequest copy = new BookingRequest();
            copy.setResourceId(baseRequest.getResourceId());
            copy.setUserId(baseRequest.getUserId());
            copy.setPurpose(baseRequest.getPurpose() + " (Week " + (i + 1) + ")");
            copy.setStartTime(baseRequest.getStartTime().plusWeeks(i));
            copy.setEndTime(baseRequest.getEndTime().plusWeeks(i));

            Object result = bookingService.createBooking(copy);

            if (result instanceof String) {
                results.add("Week " + (i + 1) + ": " + result);
            } else {
                results.add("Week " + (i + 1) + ": CONFLICT — added to waitlist");
            }
        }

        return results;
    }
}