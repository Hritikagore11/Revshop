package revshop.product_service.product.service;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import revshop.product_service.product.model.Product;
import revshop.product_service.product.repository.ProductRepository;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        )
                );
    }

    public Product updateProduct(Long id, Product product) {

        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        )
                );

        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setDiscount(product.getDiscount());
        existingProduct.setQuantity(product.getQuantity());
        existingProduct.setSellerId(product.getSellerId());
        existingProduct.setCategory(product.getCategory());

        return productRepository.save(existingProduct);
    }

    public void deleteProduct(Long id) {

        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found"
            );
        }

        productRepository.deleteById(id);
    }

    public Product reduceStock(Long productId, Integer quantity) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        )
                );

        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Invalid quantity");
        }

        if (product.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        product.setQuantity(product.getQuantity() - quantity);

        return productRepository.save(product);
    }
}