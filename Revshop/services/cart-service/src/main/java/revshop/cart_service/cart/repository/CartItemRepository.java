package revshop.cart_service.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import revshop.cart_service.cart.model.CartItem;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}
