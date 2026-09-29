package revshop.notification.service;

import org.springframework.stereotype.Service;
import revshop.notification.exception.InvalidNotificationException;
import revshop.notification.exception.NotificationNotFoundException;
import revshop.notification.model.Notification;
import revshop.notification.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository) {

        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getNotificationsByUser(
            Long userId) {

        if (userId == null) {
            throw new InvalidNotificationException(
                    "User ID is required"
            );
        }

        return notificationRepository.findByUserId(userId);
    }

    public List<Notification> getUnreadNotifications(
            Long userId) {

        if (userId == null) {
            throw new InvalidNotificationException(
                    "User ID is required"
            );
        }

        return notificationRepository
                .findByUserIdAndIsReadFalse(userId);
    }

    public List<Notification> getNotificationsByType(
            Long userId,
            String type) {

        if (userId == null) {
            throw new InvalidNotificationException(
                    "User ID is required"
            );
        }

        if (type == null || type.trim().isEmpty()) {
            throw new InvalidNotificationException(
                    "Notification type is required"
            );
        }

        return notificationRepository
                .findByUserIdAndType(userId, type);
    }

    public Notification markAsRead(
            Long notificationId) {

        if (notificationId == null) {
            throw new InvalidNotificationException(
                    "Notification ID is required"
            );
        }

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() ->
                                new NotificationNotFoundException(
                                        "Notification not found with id: "
                                                + notificationId
                                )
                        );

        notification.setRead(true);

        return notificationRepository.save(notification);
    }

    public List<Notification> markAllAsRead(
            Long userId) {

        if (userId == null) {
            throw new InvalidNotificationException(
                    "User ID is required"
            );
        }

        List<Notification> notifications =
                notificationRepository.findByUserId(userId);

        for (Notification notification : notifications) {
            notification.setRead(true);
        }

        return notificationRepository.saveAll(notifications);
    }

    public Notification createNotification(
            Notification notification) {

        if (notification == null) {
            throw new InvalidNotificationException(
                    "Notification data is required"
            );
        }

        if (notification.getUser() == null) {
            throw new InvalidNotificationException(
                    "User is required"
            );
        }

        if (notification.getMessage() == null ||
                notification.getMessage().trim().isEmpty()) {

            throw new InvalidNotificationException(
                    "Notification message is required"
            );
        }

        if (notification.getType() == null ||
                notification.getType().trim().isEmpty()) {

            throw new InvalidNotificationException(
                    "Notification type is required"
            );
        }

        notification.setRead(false);

        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(
                    LocalDateTime.now()
            );
        }

        return notificationRepository.save(notification);
    }
}