package revshop.cart_service.cart.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import revshop.cart_service.cart.model.Product;

@FeignClient(
        name = "product-service",
        url = "${product.service.url}"
)
public interface ProductClient {
    @GetMapping("/api/products/internal/{productId}")
    Product getProduct(
            @PathVariable Long productId,
            @RequestHeader("X-Internal-Key") String internalKey
    );
}