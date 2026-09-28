package revshop.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import revshop.cart.model.CartItem;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartId(Long cartId);
}