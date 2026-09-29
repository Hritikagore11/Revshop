package revshop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import revshop.notification.exception.InvalidNotificationException;
import revshop.notification.exception.NotificationNotFoundException;
import revshop.notification.model.Notification;
import revshop.notification.repository.NotificationRepository;
import revshop.notification.service.NotificationService;
import revshop.user.model.User;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification;

    @BeforeEach
    void setUp() {

        User user = new User();
        user.setId(1L);

        notification = new Notification();

        notification.setId(1L);
        notification.setUser(user);
        notification.setMessage(
                "Your order has been delivered"
        );
        notification.setType(
                "ORDER_STATUS"
        );
        notification.setRead(false);
    }


    @Test
    void createNotification_WithValidData_ShouldCreate()
            throws Exception {

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        Notification result =
                notificationService
                        .createNotification(notification);

        assertNotNull(result);
        assertFalse(result.isRead());
        assertNotNull(result.getCreatedAt());

        verify(notificationRepository)
                .save(notification);
    }


    @Test
    void createNotification_WithEmptyMessage_ShouldThrowException() {

        notification.setMessage("");

        assertThrows(
                InvalidNotificationException.class,
                () -> notificationService
                        .createNotification(notification)
        );
    }


    @Test
    void createNotification_WithEmptyType_ShouldThrowException() {

        notification.setType("");

        assertThrows(
                InvalidNotificationException.class,
                () -> notificationService
                        .createNotification(notification)
        );
    }


    @Test
    void getNotificationsByUser_ShouldReturnNotifications() {

        when(notificationRepository
                .findByUserId(1L))
                .thenReturn(
                        List.of(notification)
                );

        List<Notification> result =
                notificationService
                        .getNotificationsByUser(1L);

        assertNotNull(result);
        assertEquals(1, result.size());

        assertEquals(
                "ORDER_STATUS",
                result.get(0).getType()
        );
    }


    @Test
    void getUnreadNotifications_ShouldReturnUnread() {

        when(notificationRepository
                .findByUserIdAndIsReadFalse(1L))
                .thenReturn(
                        List.of(notification)
                );

        List<Notification> result =
                notificationService
                        .getUnreadNotifications(1L);

        assertEquals(1, result.size());

        assertFalse(
                result.get(0).isRead()
        );
    }


    @Test
    void markAsRead_ShouldMarkNotificationAsRead() {

        when(notificationRepository
                .findById(1L))
                .thenReturn(
                        Optional.of(notification)
                );

        when(notificationRepository
                .save(any(Notification.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        Notification result =
                notificationService.markAsRead(1L);

        assertTrue(
                result.isRead()
        );

        verify(notificationRepository)
                .save(notification);
    }


    @Test
    void markAsRead_WhenNotificationDoesNotExist() {

        when(notificationRepository
                .findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService
                        .markAsRead(99L)
        );
    }


    @Test
    void markAllAsRead_ShouldMarkAllAsRead() {

        Notification second =
                new Notification();

        second.setId(2L);
        second.setUser(
                notification.getUser()
        );
        second.setMessage(
                "Payment confirmed"
        );
        second.setType(
                "PAYMENT"
        );
        second.setRead(false);

        List<Notification> notifications =
                List.of(
                        notification,
                        second
                );

        when(notificationRepository
                .findByUserId(1L))
                .thenReturn(notifications);

        when(notificationRepository
                .saveAll(notifications))
                .thenReturn(notifications);

        List<Notification> result =
                notificationService
                        .markAllAsRead(1L);

        assertTrue(
                result.get(0).isRead()
        );

        assertTrue(
                result.get(1).isRead()
        );

        verify(notificationRepository)
                .saveAll(notifications);
    }


    @Test
    void getNotificationsByUser_WithNullUserId()
            throws Exception {

        assertThrows(
                InvalidNotificationException.class,
                () -> notificationService
                        .getNotificationsByUser(null)
        );
    }
}