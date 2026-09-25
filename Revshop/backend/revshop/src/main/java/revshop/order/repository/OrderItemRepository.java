package revshop.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import revshop.order.model.OrderItem;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);
}