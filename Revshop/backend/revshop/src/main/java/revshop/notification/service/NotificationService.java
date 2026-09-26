package revshop.notification.service;

import org.springframework.stereotype.Service;
import revshop.notification.model.Notification;
import revshop.notification.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }


    public List<Notification> getNotificationsByUser(Long userId) {

        return notificationRepository.findByUserId(userId);
    }


    public List<Notification> getUnreadNotifications(Long userId) {

        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }


    public List<Notification> getNotificationsByType(
            Long userId,
            String type) {

        return notificationRepository.findByUserIdAndType(
                userId,
                type
        );
    }


    public Notification markAsRead(Long notificationId) {

        Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"
                                )
                        );

        notification.setRead(true);

        return notificationRepository.save(notification);
    }


    public List<Notification> markAllAsRead(Long userId) {

        List<Notification> notifications =
                notificationRepository.findByUserId(userId);

        for (Notification notification : notifications) {
            notification.setRead(true);
        }

        return notificationRepository.saveAll(notifications);
    }


    public Notification createNotification(
            Notification notification) {

        if (notification.getMessage() == null ||
                notification.getMessage().trim().isEmpty()) {

            throw new RuntimeException(
                    "Notification message is required"
            );
        }

        if (notification.getType() == null ||
                notification.getType().trim().isEmpty()) {

            throw new RuntimeException(
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