package revshop.product_service.product.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import revshop.product_service.product.model.Product;
import revshop.product_service.product.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @Value("${product.internal.key:revshop-internal-2026}")
    private String internalKey;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Product> createProduct(
            @RequestBody Product product,
            Authentication authentication) {

        Long sellerId = getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                productService.createProduct(product, sellerId)
        );
    }


    @GetMapping
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    public ResponseEntity<List<Product>> getAllProducts() {

        return ResponseEntity.ok(
                productService.getAllProducts()
        );
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestParam String keyword) {

        return ResponseEntity.ok(
                productService.searchProducts(keyword));
    }

    @GetMapping("/category")
    public ResponseEntity<List<Product>> getProductsByCategory(
            @RequestParam String category) {

        return ResponseEntity.ok(
                productService.getProductsByCategory(category)
        );
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

        return ResponseEntity.ok(
                productService.getProductById(id)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody Product product,
            Authentication authentication) {

        Long sellerId = getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                productService.updateProduct(
                        id,
                        product,
                        sellerId
                )
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            Authentication authentication) {

        Long sellerId = getAuthenticatedUserId(authentication);

        productService.deleteProduct(id, sellerId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/inventory")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Product> updateInventory(
            @PathVariable Long id,
            @RequestParam Integer quantity,
            Authentication authentication) {

        Long sellerId = getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                productService.updateInventory(
                        id,
                        quantity,
                        sellerId
                )
        );
    }


    @PutMapping("/{id}/discount")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Product> updateDiscount(
            @PathVariable Long id,
            @RequestParam Double discount,
            Authentication authentication) {

        Long sellerId = getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                productService.updateDiscount(
                        id,
                        discount,
                        sellerId
                )
        );
    }

    @PutMapping("/internal/{id}/stock")
    public ResponseEntity<Product> reduceStockInternal(
            @PathVariable Long id,
            @RequestParam Integer quantity,
            @RequestHeader(value = "X-Internal-Key", required = false) String key) {

        if (key == null || !key.equals(internalKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (quantity == null || quantity <= 0) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                productService.reduceStock(id, quantity)
        );
    }


    @GetMapping("/internal/{id}")
    public ResponseEntity<Product> getProductInternal(
            @PathVariable Long id,
            @RequestHeader(
                    value = "X-Internal-Key",
                    required = false
            ) String key) {

        if (!internalKey.equals(key)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(
                productService.getProductById(id)
        );
    }

    private Long getAuthenticatedUserId(
            Authentication authentication) {

        if (authentication == null ||
                authentication.getDetails() == null) {

            throw new RuntimeException(
                    "User is not authenticated"
            );
        }

        Object details = authentication.getDetails();

        if (!(details instanceof Long userId)) {

            throw new RuntimeException(
                    "Unable to identify authenticated user"
            );
        }

        return userId;
    }

    @PutMapping("/{id}/stock")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Product> updateStock(
            @PathVariable Long id,
            @RequestParam Integer quantity,
            Authentication authentication) {

        Long sellerId = getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                productService.updateStock(
                        id,
                        quantity,
                        sellerId
                )
        );
    }
    @PutMapping("/internal/{id}/restore-stock")
    public ResponseEntity<Product> restoreStockInternal(
            @PathVariable Long id,
            @RequestParam Integer quantity,
            @RequestHeader(
                    value = "X-Internal-Key",
                    required = false
            ) String key) {

        if (!internalKey.equals(key)) {
            return ResponseEntity.status(403).build();
        }
        if (quantity == null || quantity <= 0) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                productService.restoreStock(id, quantity)
        );
    }


}
