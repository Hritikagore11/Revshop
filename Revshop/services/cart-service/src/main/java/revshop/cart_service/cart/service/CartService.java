package revshop.cart_service.cart.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import revshop.cart_service.cart.dto.ProductResponse;
import revshop.cart_service.cart.model.Cart;
import revshop.cart_service.cart.model.CartItem;
import revshop.cart_service.cart.model.Product;
import revshop.cart_service.cart.repository.CartItemRepository;
import revshop.cart_service.cart.repository.CartRepository;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${product.internal.key}")
    private String productInternalKey;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            RestTemplate restTemplate) {

        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.restTemplate = restTemplate;
    }

    public Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getDetails() == null) {
            throw new RuntimeException("User is not authenticated");
        }

        Object details = authentication.getDetails();
        if (!(details instanceof Long userId)) {
            throw new RuntimeException("Unable to identify authenticated user");
        }

        return userId;
    }

    public CartItem addItem(
            Long userId,
            Long productId,
            Integer quantity) {

        if (productId == null) {
            throw new RuntimeException("Product ID is required");
        }

        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Invalid quantity");
        }

        Product product = getProduct(productId);

        if (product.getQuantity() != null && quantity > product.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    return cartRepository.save(newCart);
                });

        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElseGet(CartItem::new);

        cartItem.setCart(cart);
        cartItem.setProductId(productId);

        int newQuantity = (cartItem.getId() == null ? 0 : cartItem.getQuantity()) + quantity;

        if (product.getQuantity() != null && newQuantity > product.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }

        cartItem.setQuantity(newQuantity);

        return cartItemRepository.save(cartItem);
    }

    public List<CartItem> getCartItems(Long userId) {

        return cartRepository.findByUserId(userId)
                .map(cart -> cartItemRepository.findByCartId(cart.getId()))
                .orElseGet(List::of);
    }

    public void removeItem(Long itemId, Long userId) {

        CartItem existingItem = getOwnedCartItem(itemId, userId);
        cartItemRepository.delete(existingItem);
    }

    public CartItem updateItem(
            Long itemId,
            Integer quantity,
            Long userId) {

        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        CartItem existingItem = getOwnedCartItem(itemId, userId);

        Product product = getProduct(existingItem.getProductId());

        if (product.getQuantity() != null && quantity > product.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }

        existingItem.setQuantity(quantity);
        return cartItemRepository.save(existingItem);
    }

    public Double getCartTotal(Long userId, String authorizationHeader) {

        List<CartItem> cartItems = getCartItems(userId);
        double total = 0.0;

        for (CartItem item : cartItems) {
            Product product = getProduct(item.getProductId(), authorizationHeader);

            double price = product.getPrice() != null ? product.getPrice() : 0.0;
            double discount = product.getDiscount() != null ? product.getDiscount() : 0.0;
            double finalPrice = price - (price * discount / 100.0);

            total += finalPrice * item.getQuantity();
        }

        return total;
    }

    public void clearCart(Long userId) {

        cartRepository.findByUserId(userId).ifPresent(cart ->
                cartItemRepository.deleteAll(
                        cartItemRepository.findByCartId(cart.getId())
                )
        );
    }

    private CartItem getOwnedCartItem(Long itemId, Long userId) {

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() ->
                        new RuntimeException("Cart item not found"));

        if (item.getCart() == null ||
                !userId.equals(item.getCart().getUserId())) {
            throw new RuntimeException("You are not authorized to modify this cart item");
        }

        return item;
    }

    private Product getProduct(Long productId) {
        return getProduct(productId, null);
    }

    private Product getProduct(Long productId, String authorizationHeader) {

        String url = productServiceUrl + "/api/products/internal/" + productId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Key", productInternalKey);

        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set("Authorization", authorizationHeader);
        }

        try {
            ResponseEntity<Product> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Product.class
            );

            Product product = response.getBody();
            if (product == null) {
                throw new RuntimeException("Product not found: " + productId);
            }

            return product;
        } catch (RestClientException e) {
            throw new RuntimeException("Product not found: " + productId);
        }
    }
}
