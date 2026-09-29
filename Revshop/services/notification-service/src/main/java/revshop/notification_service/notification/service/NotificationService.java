package revshop.notification_service.notification.service;

import org.springframework.stereotype.Service;
import revshop.notification_service.notification.model.Notification;
import revshop.notification_service.notification.repository.NotificationRepository;

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

        return notificationRepository.findByUserId(userId);
    }

    public Notification markAsRead(
            Long notificationId) {

        Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"
                                ));

        notification.setIsRead(true);

        return notificationRepository.save(notification);
    }

    public Notification createNotification(
            Notification notification) {

        notification.setIsRead(false);

        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(
                    LocalDateTime.now()
            );
        }

        return notificationRepository.save(notification);
    }

    public List<Notification> getUnreadNotifications(
            Long userId) {

        return notificationRepository
                .findByUserIdAndIsReadFalse(userId);
    }

    public List<Notification> getNotificationsByType(
            Long userId,
            String type) {

        return notificationRepository
                .findByUserIdAndType(
                        userId,
                        type
                );
    }

    public List<Notification> markAllAsRead(
            Long userId) {

        List<Notification> notifications =
                notificationRepository.findByUserId(userId);

        for (Notification notification : notifications) {
            notification.setIsRead(true);
        }

        return notificationRepository.saveAll(
                notifications
        );
    }

    public Notification getNotificationById(
            Long notificationId) {

        return notificationRepository.findById(notificationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Notification not found"
                        ));
    }
}