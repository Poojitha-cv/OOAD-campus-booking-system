package com.campus.booking.observer;

import com.campus.booking.model.Booking;

/**
 * DESIGN PATTERN: Observer
 * All listeners that want to be notified of booking events implement this interface.
 */
public interface BookingEventObserver {
    void onBookingConfirmed(Booking booking);
    void onBookingCancelled(Booking booking);
    void onBookingPromoted(Booking booking);    // waitlist → confirmed
    void onBookingConflict(Booking booking);
}