package revshop.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import revshop.notification.model.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(Long userId);

    List<Notification> findByUserIdAndIsReadFalse(Long userId);

    List<Notification> findByUserIdAndType(Long userId, String type);
}