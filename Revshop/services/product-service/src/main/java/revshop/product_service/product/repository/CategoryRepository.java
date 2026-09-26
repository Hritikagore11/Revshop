package revshop.product_service.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import revshop.product_service.product.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);
}