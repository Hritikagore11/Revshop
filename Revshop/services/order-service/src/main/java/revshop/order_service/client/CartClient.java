package revshop.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import revshop.order_service.order.dto.CartItemResponse;

@FeignClient(name = "cart-service")
public interface CartClient {

    @GetMapping("/cart")
    CartItemResponse[] getCart(
            @RequestHeader("Authorization") String authorization
    );

    @DeleteMapping("/cart/clear")
    void clearCart(
            @RequestHeader("Authorization") String authorization
    );
}