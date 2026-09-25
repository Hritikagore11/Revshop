package revshop.product_service.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import revshop.product_service.product.model.Product;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByCategoryId(Long categoryId);
}