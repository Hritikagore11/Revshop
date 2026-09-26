package revshop.order_service.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import revshop.order_service.order.model.Order;
import revshop.order_service.order.model.OrderItem;
import revshop.order_service.order.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Order> createOrder(
            @RequestParam Long userId,
            @RequestParam Double totalAmount) {

        return ResponseEntity.ok(
                orderService.createOrder(userId, totalAmount)
        );
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<Order>> getOrdersByUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                orderService.getOrdersByUser(userId)
        );
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Order> getOrderById(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(
                orderService.getOrderById(orderId)
        );
    }

    @PostMapping("/{orderId}/items")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<OrderItem> addOrderItem(
            @PathVariable Long orderId,
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            @RequestParam Double price) {

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
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<OrderItem>> getOrderItems(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(
                orderService.getOrderItems(orderId)
        );
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable long orderId, @RequestParam String status){
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }
}