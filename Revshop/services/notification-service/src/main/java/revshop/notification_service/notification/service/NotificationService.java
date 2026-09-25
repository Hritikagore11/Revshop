package revshop.notification_service.notification.service;

import org.springframework.stereotype.Service;
import revshop.notification_service.notification.model.Notification;
import revshop.notification_service.notification.repository.NotificationRepository;

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

    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setIsRead(true);

        return notificationRepository.save(notification);
    }

    public Notification createNotification(Notification notification) {
        notification.setIsRead(false);

        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(java.time.LocalDateTime.now());
        }

        return notificationRepository.save(notification);
    }
}