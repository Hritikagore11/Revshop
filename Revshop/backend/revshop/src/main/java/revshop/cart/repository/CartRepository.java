package revshop.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import revshop.cart.model.Cart;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);
}