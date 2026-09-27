package revshop.order_service.order.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import revshop.order_service.order.dto.CheckoutRequest;
import revshop.order_service.order.model.Order;
import revshop.order_service.order.model.OrderItem;
import revshop.order_service.order.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestParam Double totalAmount,
            Authentication authentication) {

        Long userId = orderService.getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                orderService.createOrder(userId, totalAmount)
        );
    }

    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout(
            @RequestBody CheckoutRequest request,
            Authentication authentication,
            @RequestHeader("Authorization") String authorization) {

        Long userId = orderService.getAuthenticatedUserId(authentication);

        Order order = orderService.checkout(
                userId,
                request.getPaymentMethod(),
                authorization
        );

        return ResponseEntity.ok(order);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUser(
            @PathVariable Long userId,
            Authentication authentication) {

        Long authenticatedUserId =
                orderService.getAuthenticatedUserId(authentication);

        if (!authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(
                orderService.getOrdersByUser(userId)
        );
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(
            @PathVariable Long orderId,
            Authentication authentication) {

        Long authenticatedUserId =
                orderService.getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                orderService.getOrderById(
                        orderId,
                        authenticatedUserId
                )
        );
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderItem> addOrderItem(
            @PathVariable Long orderId,
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            @RequestParam Double price,
            Authentication authentication) {

        Long authenticatedUserId =
                orderService.getAuthenticatedUserId(authentication);

        orderService.getOrderById(orderId, authenticatedUserId);

        return ResponseEntity.ok(
                orderService.addOrderItem(
                        orderId,
                        productId,
                        quantity,
                        price
                )
        );
    }

    @GetMapping("/{orderId}/items")
    public ResponseEntity<List<OrderItem>> getOrderItems(
            @PathVariable Long orderId,
            Authentication authentication) {

        Long authenticatedUserId =
                orderService.getAuthenticatedUserId(authentication);

        orderService.getOrderById(orderId, authenticatedUserId);

        return ResponseEntity.ok(
                orderService.getOrderItems(orderId)
        );
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status,
            Authentication authentication) {

        return ResponseEntity.ok(
                orderService.updateOrderStatus(
                        orderId,
                        status,
                        authentication
                )
        );
    }
}