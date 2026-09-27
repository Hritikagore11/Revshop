package revshop.notification_service.notification.controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import revshop.notification_service.notification.model.Notification;
import revshop.notification_service.notification.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getNotifications(
            @PathVariable Long userId,
            Authentication authentication) {

        if (!isOwner(userId, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(
                notificationService.getNotificationsByUser(userId)
        );
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(
            @PathVariable Long userId,
            Authentication authentication) {

        if (!isOwner(userId, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(
                notificationService.getUnreadNotifications(userId)
        );
    }

    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<Notification>> getNotificationsByType(
            @PathVariable Long userId,
            @PathVariable String type,
            Authentication authentication) {

        if (!isOwner(userId, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(
                notificationService.getNotificationsByType(userId, type)
        );
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<List<Notification>> markAllAsRead(
            @PathVariable Long userId,
            Authentication authentication) {

        if (!isOwner(userId, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(
                notificationService.markAllAsRead(userId)
        );
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        Long loggedInUserId = getUserId(authentication);

        Notification notification =
                notificationService.getNotificationById(id);

        if (!notification.getUserId().equals(loggedInUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(
                notificationService.markAsRead(id)
        );
    }

    @PostMapping
    public ResponseEntity<Notification> createNotification(
            @RequestBody Notification notification) {

        return ResponseEntity.ok(
                notificationService.createNotification(notification)
        );
    }

    private boolean isOwner(
            Long requestedUserId,
            Authentication authentication) {

        Long loggedInUserId = getUserId(authentication);

        return loggedInUserId != null
                && loggedInUserId.equals(requestedUserId);
    }

    private Long getUserId(Authentication authentication) {

        if (authentication == null ||
                authentication.getDetails() == null) {
            return null;
        }

        return (Long) authentication.getDetails();
    }
}
