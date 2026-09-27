package revshop.product_service.product.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import revshop.product_service.product.model.Product;
import revshop.product_service.product.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @Value("${product.internal.key}")
    private String internalKey;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Product> createProduct(
            @RequestBody Product product) {

        return ResponseEntity.ok(
                productService.createProduct(product)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestParam String name) {
        return ResponseEntity.ok(productService.searchProducts(name));
    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    public ResponseEntity<List<Product>> getProductsByCategory(
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(
                productService.getProductsByCategory(categoryId)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    public ResponseEntity<Product> getProductById(
            @PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody Product product) {
        return ResponseEntity.ok(
                productService.updateProduct(id, product)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/stock")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Product> reduceStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(
                productService.reduceStock(id, quantity)
        );
    }

    @GetMapping("/internal/{id}")
    public ResponseEntity<Product> getProductInternal(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Key", required = false) String key) {

        if (!internalKey.equals(key)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(
                productService.getProductById(id)
        );
    }

    @PutMapping("/internal/{id}/stock")
    public ResponseEntity<Product> reduceStockInternal(
            @PathVariable Long id,
            @RequestParam Integer quantity,
            @RequestHeader(value = "X-Internal-Key", required = false) String key) {

        if (!internalKey.equals(key)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(
                productService.reduceStock(id, quantity)
        );
    }
}
