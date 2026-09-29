package revshop.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import revshop.order_service.order.dto.ProductResponse;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/products/internal/{id}")
    ProductResponse getProduct(
            @PathVariable("id") Long id,
            @RequestHeader("X-Internal-Key") String internalKey
    );
}