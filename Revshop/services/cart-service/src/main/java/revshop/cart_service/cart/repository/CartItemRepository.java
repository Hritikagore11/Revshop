package revshop.cart_service.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import revshop.cart_service.cart.model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}