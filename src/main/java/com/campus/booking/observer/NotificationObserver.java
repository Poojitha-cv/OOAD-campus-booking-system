package com.campus.booking.observer;

import com.campus.booking.model.Booking;
import com.campus.booking.model.Notification;
import com.campus.booking.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * DESIGN PATTERN: Observer (Concrete Observer)
 * Listens to all booking events and persists notifications to the database.
 */
@Component
public class NotificationObserver implements BookingEventObserver {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public void onBookingConfirmed(Booking booking) {
        save(booking, "✅ Your booking for " + booking.getResource().getName()
                + " from " + booking.getStartTime() + " to " + booking.getEndTime()
                + " has been CONFIRMED.", "CONFIRMED");
    }

    @Override
    public void onBookingCancelled(Booking booking) {
        save(booking, "❌ Your booking for " + booking.getResource().getName()
                + " on " + booking.getStartTime().toLocalDate()
                + " has been CANCELLED.", "CANCELLED");
    }

    @Override
    public void onBookingPromoted(Booking booking) {
        save(booking, "🎉 Great news! Your waitlisted booking for "
                + booking.getResource().getName()
                + " has been PROMOTED to CONFIRMED.", "PROMOTED");
    }

    @Override
    public void onBookingConflict(Booking booking) {
        save(booking, "⚠️ Your booking for " + booking.getResource().getName()
                + " could not be confirmed due to a conflict. You have been added to the waitlist.", "CONFLICT");
    }

    private void save(Booking booking, String message, String type) {
        Notification n = new Notification();
        n.setUser(booking.getUser());
        n.setMessage(message);
        n.setType(type);
        notificationRepository.save(n);
    }
}