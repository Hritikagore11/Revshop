package revshop.order_service.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import revshop.order_service.order.model.Order;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);
}