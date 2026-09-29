package revshop.product_service.product.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/notifications/internal")
    void createNotification(
            @RequestBody NotificationRequest request,
            @RequestHeader("X-Internal-Key") String internalKey
    );

    record NotificationRequest(
            Long userId,
            String message,
            String type,
            boolean isRead
    ) {
    }
}