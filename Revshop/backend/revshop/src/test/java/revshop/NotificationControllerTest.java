package revshop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import revshop.notification.controller.NotificationController;
import revshop.notification.model.Notification;
import revshop.notification.service.NotificationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;


    // GET /notifications/user/{userId}
    @Test
    void getNotifications_ShouldReturn200()
            throws Exception {

        Notification notification =
                new Notification();

        notification.setId(1L);
        notification.setMessage(
                "Your order has been delivered"
        );
        notification.setType(
                "ORDER_STATUS"
        );
        notification.setRead(false);

        when(notificationService
                .getNotificationsByUser(1L))
                .thenReturn(
                        List.of(notification)
                );

        mockMvc.perform(
                        get("/notifications/user/1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(
                        jsonPath("$[0].message")
                                .value(
                                        "Your order has been delivered"
                                )
                )
                .andExpect(
                        jsonPath("$[0].type")
                                .value("ORDER_STATUS")
                )
                .andExpect(
                        jsonPath("$[0].isRead")
                                .value(false)
                );
    }


    // PUT /notifications/{id}/read
    @Test
    void markAsRead_ShouldReturn200()
            throws Exception {

        Notification notification =
                new Notification();

        notification.setId(1L);
        notification.setMessage(
                "Your order has been delivered"
        );
        notification.setType(
                "ORDER_STATUS"
        );
        notification.setRead(true);

        when(notificationService
                .markAsRead(1L))
                .thenReturn(notification);

        mockMvc.perform(
                        put("/notifications/1/read")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.isRead")
                                .value(true)
                );
    }


    // POST /notifications
    @Test
    void createNotification_ShouldReturn200()
            throws Exception {

        Notification notification =
                new Notification();

        notification.setId(1L);
        notification.setMessage(
                "Your order has been delivered"
        );
        notification.setType(
                "ORDER_STATUS"
        );
        notification.setRead(false);

        when(notificationService
                .createNotification(any(Notification.class)))
                .thenReturn(notification);

        mockMvc.perform(
                        post("/notifications")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                {
                                    "message":
                                      "Your order has been delivered",
                                    "type":
                                      "ORDER_STATUS"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Your order has been delivered"
                                )
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("ORDER_STATUS")
                )
                .andExpect(
                        jsonPath("$.isRead")
                                .value(false)
                );
    }
}