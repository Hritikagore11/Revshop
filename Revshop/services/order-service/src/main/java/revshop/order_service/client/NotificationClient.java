package revshop.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/notifications")
    void createNotification(
            @RequestBody NotificationRequest request,
            @RequestHeader("Authorization") String authorization
    );

    class NotificationRequest {

        private Long userId;
        private String message;
        private String type;
        private Boolean isRead;

        public NotificationRequest() {
        }

        public NotificationRequest(
                Long userId,
                String message,
                String type,
                Boolean isRead) {

            this.userId = userId;
            this.message = message;
            this.type = type;
            this.isRead = isRead;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean getIsRead() {
            return isRead;
        }

        public void setIsRead(Boolean isRead) {
            this.isRead = isRead;
        }
    }
}