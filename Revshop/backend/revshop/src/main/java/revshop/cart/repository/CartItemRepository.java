package revshop.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import revshop.cart.model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}