package revshop.product_service.product.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import revshop.product_service.product.client.NotificationClient;
import revshop.product_service.product.model.Product;
import revshop.product_service.product.repository.ProductRepository;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;


    @Value("${notification.internal.key}")
    private String notificationInternalKey;

    private final NotificationClient notificationClient;

    public ProductService(
            ProductRepository productRepository,
            NotificationClient notificationClient) {

        this.productRepository = productRepository;
        this.notificationClient = notificationClient;
    }

    public Product createProduct(
            Product product,
            Long sellerId) {

        validateProduct(product);

        product.setSellerId(sellerId);

        if (product.getLowStockThreshold() == null) {
            product.setLowStockThreshold(5);
        }

        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Page<Product> getProductsPage(int page, int size) {

        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and 50"
            );
        }

        return productRepository.findAll(
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.ASC, "id")
                )
        );
    }

    public List<Product> searchProducts(String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Search keyword is required"
            );
        }

        return productRepository
                .findByNameContainingIgnoreCase(keyword);
    }

    public List<Product> getProductsByCategory(
            Long categoryId) {

        return productRepository
                .findByCategoryId(categoryId);
    }

    public Product getProductById(Long id) {

        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        ));
    }

    public Product updateProduct(
            Long id,
            Product product,
            Long sellerId) {

        Product existingProduct =
                productRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Product not found"
                                ));

        checkOwnership(existingProduct, sellerId);

        validateProduct(product);

        int previousQuantity =
                existingProduct.getQuantity() == null
                        ? 0
                        : existingProduct.getQuantity();

        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setImageUrl(product.getImageUrl());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setDiscount(product.getDiscount());
        existingProduct.setQuantity(product.getQuantity());
        existingProduct.setCategory(product.getCategory());

        if (product.getLowStockThreshold() != null) {
            validateLowStockThreshold(
                    product.getLowStockThreshold()
            );

            existingProduct.setLowStockThreshold(
                    product.getLowStockThreshold()
            );
        } else if (existingProduct.getLowStockThreshold() == null) {
            existingProduct.setLowStockThreshold(5);
        }

        existingProduct.setSellerId(sellerId);

        Product savedProduct =
                productRepository.save(existingProduct);

        notifyIfStockBecameLow(
                savedProduct,
                previousQuantity
        );

        return savedProduct;
    }

    public void deleteProduct(
            Long id,
            Long sellerId) {

        Product product =
                productRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Product not found"
                                ));

        checkOwnership(product, sellerId);

        productRepository.delete(product);
    }

    public Product updateInventory(
            Long productId,
            Integer quantity,
            Long sellerId) {

        if (quantity == null || quantity < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity cannot be negative"
            );
        }

        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Product not found"
                                ));

        checkOwnership(product, sellerId);

        int previousQuantity =
                product.getQuantity() == null
                        ? 0
                        : product.getQuantity();

        product.setQuantity(quantity);

        if (product.getLowStockThreshold() == null) {
            product.setLowStockThreshold(5);
        }

        Product savedProduct =
                productRepository.save(product);

        notifyIfStockBecameLow(
                savedProduct,
                previousQuantity
        );

        return savedProduct;
    }

    public Product updateDiscount(
            Long productId,
            Double discount,
            Long sellerId) {

        if (discount == null ||
                discount < 0 ||
                discount > 100) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Discount must be between 0 and 100"
            );
        }

        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Product not found"
                                ));

        checkOwnership(product, sellerId);

        product.setDiscount(discount);

        return productRepository.save(product);
    }

    /*
     * =====================================================
     * CHECKOUT - REDUCE STOCK
     * =====================================================
     *
     * IMPORTANT:
     * This method is already used by checkout.
     * We keep the existing stock validation and reduction.
     *
     * The only addition is low-stock detection after reduction.
     */
    public Product reduceStock(
            Long productId,
            Integer quantity) {

        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Product not found"
                                ));

        if (quantity == null || quantity <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid quantity"
            );
        }

        if (product.getQuantity() == null ||
                product.getQuantity() < quantity) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient stock"
            );
        }

        if (product.getLowStockThreshold() == null) {
            product.setLowStockThreshold(5);
        }

        int previousQuantity =
                product.getQuantity();

        product.setQuantity(
                product.getQuantity() - quantity
        );

        Product savedProduct =
                productRepository.save(product);

        /*
         * IMPORTANT:
         * Notification failure must never break checkout.
         */
        notifyIfStockBecameLow(
                savedProduct,
                previousQuantity
        );

        return savedProduct;
    }

    private void checkOwnership(
            Product product,
            Long sellerId) {

        if (product.getSellerId() == null ||
                !product.getSellerId().equals(sellerId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to modify this product"
            );
        }
    }

    private void validateProduct(Product product) {

        if (product.getName() == null ||
                product.getName().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product name is required"
            );
        }

        if (product.getPrice() == null ||
                product.getPrice() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Price must be greater than 0"
            );
        }

        if (product.getQuantity() == null ||
                product.getQuantity() < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity cannot be negative"
            );
        }

        if (product.getDiscount() != null &&
                (product.getDiscount() < 0 ||
                        product.getDiscount() > 100)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Discount must be between 0 and 100"
            );
        }

        if (product.getLowStockThreshold() != null) {
            validateLowStockThreshold(
                    product.getLowStockThreshold()
            );
        }
    }

    private void validateLowStockThreshold(
            Integer threshold) {

        if (threshold == null || threshold < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Low-stock threshold cannot be negative"
            );
        }
    }

    public List<Product> getProductsByCategory(
            String category) {

        return productRepository
                .findByCategoryNameIgnoreCase(category);
    }

    public Product updateStock(
            Long id,
            Integer quantity,
            Long sellerId) {

        if (quantity == null || quantity < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Stock quantity cannot be negative"
            );
        }

        Product product =
                productRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Product not found"
                                ));

        checkOwnership(product, sellerId);

        if (product.getLowStockThreshold() == null) {
            product.setLowStockThreshold(5);
        }

        int previousQuantity =
                product.getQuantity() == null
                        ? 0
                        : product.getQuantity();

        product.setQuantity(quantity);

        Product savedProduct =
                productRepository.save(product);

        notifyIfStockBecameLow(
                savedProduct,
                previousQuantity
        );

        return savedProduct;
    }

    public Product restoreStock(
            Long productId,
            Integer quantity) {

        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found: " +
                                                productId
                                ));

        if (quantity == null || quantity <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found: " + productId
            );
        }

        product.setQuantity(
                product.getQuantity() + quantity
        );

        return productRepository.save(product);
    }

    /*
     * =====================================================
     * LOW-STOCK DETECTION
     * =====================================================
     *
     * We notify only when the stock crosses from above
     * the threshold to at/below the threshold.
     *
     * Example:
     *
     * threshold = 5
     *
     * 8 -> 6 : no notification
     * 6 -> 5 : notification
     * 5 -> 4 : no duplicate notification
     */
    private void notifyIfStockBecameLow(
            Product product,
            int previousQuantity) {

        Integer threshold =
                product.getLowStockThreshold();

        if (threshold == null) {
            threshold = 5;
            product.setLowStockThreshold(5);
        }

        boolean wasAboveThreshold =
                previousQuantity > threshold;

        boolean isNowLow =
                product.getQuantity() <= threshold;

        if (!wasAboveThreshold || !isNowLow) {
            return;
        }

        sendLowStockNotification(product);
    }

    /*
     * =====================================================
     * SELLER LOW-STOCK NOTIFICATION
     * =====================================================
     */
    private void sendLowStockNotification(
            Product product) {

        try {

            String message =
                    "Low stock alert: Product '" +
                            product.getName() +
                            "' has only " +
                            product.getQuantity() +
                            " item(s) remaining.";

            NotificationClient.NotificationRequest request =
                    new NotificationClient.NotificationRequest(
                            product.getSellerId(),
                            message,
                            "LOW_STOCK",
                            false
                    );

            notificationClient.createNotification(
                    request,
                    notificationInternalKey
            );

        } catch (Exception e) {

            /*
             * VERY IMPORTANT:
             * Low-stock notification is not part of the
             * checkout transaction.
             *
             * If notification-service is down, the product
             * stock reduction must still succeed.
             */
            System.out.println(
                    "Warning: Low-stock notification failed: " +
                            e.getMessage()
            );
        }
    }
}