package com.gotryt.coop.controller;

import com.gotryt.coop.model.Notification;
import com.gotryt.coop.model.User;
import com.gotryt.coop.repository.NotificationRepository;
import com.gotryt.coop.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notis")
@AllArgsConstructor
public class NotisController {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    // Get all notifications (Admin use or for debugging)
    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAllNotis() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }

    // Get all notifications for the logged-in user
    @GetMapping("/mynotis")
    public ResponseEntity<List<Notification>> getUserNotis(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserProfileByJwt(jwt).getUser();
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
    }

    // Get unread notifications for the user
    @GetMapping("/user/unread")
    public ResponseEntity<List<Notification>> getUnreadNotis(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserProfileByJwt(jwt).getUser();
        return ResponseEntity.ok(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId()));
    }

    // Mark a specific notification as read
    @PutMapping("/mark-read/{id}")
    public ResponseEntity<?> markAsRead(@RequestHeader("Authorization") String jwt, @PathVariable Long id) {
        Notification noti = notificationRepository.findById(id).orElse(null);
        if (noti != null) {
            noti.setRead(true);
            notificationRepository.save(noti);
            return ResponseEntity.ok("Notification marked as read");
        }
        return ResponseEntity.badRequest().body("Notification not found");
    }

    // Mark all notifications as read for a user
    @PutMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserProfileByJwt(jwt).getUser();
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(user.getId());
        unread.forEach(noti -> noti.setRead(true));
        notificationRepository.saveAll(unread);
        return ResponseEntity.ok("All notifications marked as read");
    }

    // Delete a specific notification
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@RequestHeader("Authorization") String jwt, @PathVariable Long id) {
        if (notificationRepository.existsById(id)) {
            notificationRepository.deleteById(id);
            return ResponseEntity.ok("Notification deleted");
        }
        return ResponseEntity.badRequest().body("Notification not found");
    }

    // Delete all notifications for a user
    @DeleteMapping("/user/delete")
    public ResponseEntity<?> deleteAllUserNotifications(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserProfileByJwt(jwt).getUser();
        notificationRepository.deleteByUserId(user.getId());
        return ResponseEntity.ok("All user notifications deleted");
    }

}
