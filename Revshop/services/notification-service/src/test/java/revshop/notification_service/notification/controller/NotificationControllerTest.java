package revshop.notification_service.notification.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import revshop.notification_service.notification.model.Notification;
import revshop.notification_service.notification.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockMvc = MockMvcBuilders
                .standaloneSetup(notificationController)
                .build();

        SecurityContextHolder.clearContext();
    }

    private UsernamePasswordAuthenticationToken buyerAuthentication() {

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "test@gmail.com",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_BUYER")
                        )
                );

        // Same userId that the controller expects from JWT filter
        authentication.setDetails(10L);

        return authentication;
    }

    private Notification createNotification() {

        Notification notification = new Notification();

        notification.setId(1L);
        notification.setUserId(10L);
        notification.setMessage("Order placed successfully");
        notification.setType("ORDER");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        return notification;
    }

    @Test
    void getNotifications_shouldReturnOk() throws Exception {

        when(notificationService.getNotificationsByUser(10L))
                .thenReturn(List.of(createNotification()));

        mockMvc.perform(
                        get("/notifications/user/10")
                                .principal(buyerAuthentication())
                )
                .andExpect(status().isOk());

        verify(notificationService)
                .getNotificationsByUser(10L);
    }

    @Test
    void getUnreadNotifications_shouldReturnOk() throws Exception {

        when(notificationService.getUnreadNotifications(10L))
                .thenReturn(List.of(createNotification()));

        mockMvc.perform(
                        get("/notifications/user/10/unread")
                                .principal(buyerAuthentication())
                )
                .andExpect(status().isOk());

        verify(notificationService)
                .getUnreadNotifications(10L);
    }

    @Test
    void getNotificationsByType_shouldReturnOk() throws Exception {

        when(notificationService.getNotificationsByType(10L, "ORDER"))
                .thenReturn(List.of(createNotification()));

        mockMvc.perform(
                        get("/notifications/user/10/type/ORDER")
                                .principal(buyerAuthentication())
                )
                .andExpect(status().isOk());

        verify(notificationService)
                .getNotificationsByType(10L, "ORDER");
    }

    @Test
    void markAllAsRead_shouldReturnOk() throws Exception {

        Notification notification = createNotification();
        notification.setIsRead(true);

        when(notificationService.markAllAsRead(10L))
                .thenReturn(List.of(notification));

        mockMvc.perform(
                        put("/notifications/user/10/read-all")
                                .principal(buyerAuthentication())
                )
                .andExpect(status().isOk());

        verify(notificationService)
                .markAllAsRead(10L);
    }

    @Test
    void markAsRead_shouldReturnOk() throws Exception {

        Notification notification = createNotification();

        when(notificationService.getNotificationById(1L))
                .thenReturn(notification);

        when(notificationService.markAsRead(1L))
                .thenReturn(notification);

        mockMvc.perform(
                        put("/notifications/1/read")
                                .principal(buyerAuthentication())
                )
                .andExpect(status().isOk());

        verify(notificationService)
                .getNotificationById(1L);

        verify(notificationService)
                .markAsRead(1L);
    }

    @Test
    void createNotification_shouldReturnOk() throws Exception {

        Notification notification = createNotification();

        when(notificationService.createNotification(any(Notification.class)))
                .thenReturn(notification);

        String json = """
                {
                    "userId": 10,
                    "message": "Order placed successfully",
                    "type": "ORDER",
                    "isRead": false
                }
                """;

        mockMvc.perform(
                        post("/notifications")
                                .contentType("application/json")
                                .content(json)
                )
                .andExpect(status().isOk());

        verify(notificationService)
                .createNotification(any(Notification.class));
    }
}