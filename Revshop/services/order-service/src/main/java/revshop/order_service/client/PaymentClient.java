package revshop.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import revshop.order_service.order.dto.PaymentResponse;

@FeignClient(name = "payment-service")
public interface PaymentClient {

    @PostMapping("/payments")
    PaymentResponse createPayment(
            @RequestParam("orderId") Long orderId,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam("amount") Double amount,
            @RequestHeader("Authorization") String authorization
    );
}