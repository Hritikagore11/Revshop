package revshop.product_service.product.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import revshop.product_service.product.model.Review;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductId(Long productId);

    boolean existsByProductIdAndBuyerId(Long productId, Long buyerId);
}
