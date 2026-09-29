package revshop.notification_service.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import revshop.notification_service.notification.model.Notification;
import revshop.notification_service.notification.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification;

    @BeforeEach
    void setUp() {

        notification = new Notification(
                1L,
                10L,
                "Order #1 placed successfully",
                "ORDER",
                false,
                LocalDateTime.now()
        );
    }

    @Test
    void getNotificationsByUser_shouldReturnNotifications() {

        when(notificationRepository.findByUserId(10L))
                .thenReturn(List.of(notification));

        List<Notification> result =
                notificationService.getNotificationsByUser(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getUserId());

        verify(notificationRepository)
                .findByUserId(10L);
    }

    @Test
    void createNotification_shouldSetUnreadAndCreatedAt() {

        Notification newNotification =
                new Notification(
                        null,
                        10L,
                        "Order placed",
                        "ORDER",
                        true,
                        null
                );

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        Notification result =
                notificationService.createNotification(
                        newNotification
                );

        assertNotNull(result);
        assertFalse(result.getIsRead());
        assertNotNull(result.getCreatedAt());

        verify(notificationRepository)
                .save(newNotification);
    }

    @Test
    void markAsRead_shouldSetIsReadTrue() {

        notification.setIsRead(false);

        when(notificationRepository.findById(1L))
                .thenReturn(Optional.of(notification));

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(notification);

        Notification result =
                notificationService.markAsRead(1L);

        assertTrue(result.getIsRead());

        verify(notificationRepository)
                .findById(1L);

        verify(notificationRepository)
                .save(notification);
    }

    @Test
    void markAsRead_shouldThrowWhenNotificationNotFound() {

        when(notificationRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> notificationService.markAsRead(99L)
                );

        assertEquals(
                "Notification not found",
                exception.getMessage()
        );
    }

    @Test
    void getUnreadNotifications_shouldReturnUnreadNotifications() {

        when(notificationRepository
                .findByUserIdAndIsReadFalse(10L))
                .thenReturn(List.of(notification));

        List<Notification> result =
                notificationService
                        .getUnreadNotifications(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsRead());

        verify(notificationRepository)
                .findByUserIdAndIsReadFalse(10L);
    }

    @Test
    void getNotificationsByType_shouldReturnMatchingNotifications() {

        when(notificationRepository
                .findByUserIdAndType(10L, "ORDER"))
                .thenReturn(List.of(notification));

        List<Notification> result =
                notificationService
                        .getNotificationsByType(
                                10L,
                                "ORDER"
                        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORDER", result.get(0).getType());

        verify(notificationRepository)
                .findByUserIdAndType(10L, "ORDER");
    }

    @Test
    void markAllAsRead_shouldMarkAllNotificationsRead() {

        Notification second =
                new Notification(
                        2L,
                        10L,
                        "Low stock",
                        "LOW_STOCK",
                        false,
                        LocalDateTime.now()
                );

        when(notificationRepository.findByUserId(10L))
                .thenReturn(List.of(notification, second));

        when(notificationRepository.saveAll(anyList()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        List<Notification> result =
                notificationService.markAllAsRead(10L);

        assertEquals(2, result.size());
        assertTrue(result.get(0).getIsRead());
        assertTrue(result.get(1).getIsRead());

        verify(notificationRepository)
                .findByUserId(10L);

        verify(notificationRepository)
                .saveAll(anyList());
    }
}