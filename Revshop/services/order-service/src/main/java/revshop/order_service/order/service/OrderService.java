package revshop.order_service.order.service;

import org.aspectj.weaver.ast.Or;
import org.springframework.stereotype.Service;
import revshop.order_service.order.model.Order;
import revshop.order_service.order.model.OrderItem;
import revshop.order_service.order.repository.OrderItemRepository;
import revshop.order_service.order.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public Order createOrder(Long userId, Double totalAmount) {

        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("PLACED");
        order.setCreatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public OrderItem addOrderItem(Long orderId,
                                  Long productId,
                                  Integer quantity,
                                  Double price) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProductId(productId);
        orderItem.setQuantity(quantity);
        orderItem.setPrice(price);

        return orderItemRepository.save(orderItem);
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public Order updateOrderStatus(Long orderId, String status){
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(status);
        return orderRepository.save(order);
    }
}