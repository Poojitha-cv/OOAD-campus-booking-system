package com.campus.booking.controller;

import com.campus.booking.model.Notification;
import com.campus.booking.repository.NotificationRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MVC Controller for the Notification inbox (Observer pattern output).
 * Pragna's Minor 2 — Notification System UI.
 */
@Controller
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping
    public String viewNotifications(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/";

        List<Notification> notifications =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // Mark all as read
        notifications.stream().filter(n -> !n.isRead()).forEach(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });

        long unread = notificationRepository.countByUserIdAndRead(userId, false);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unread);
        return "notifications";
    }
}