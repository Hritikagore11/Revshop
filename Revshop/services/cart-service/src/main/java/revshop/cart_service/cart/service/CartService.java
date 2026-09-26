package revshop.cart_service.cart.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;
import revshop.cart_service.cart.model.Cart;
import revshop.cart_service.cart.model.CartItem;
import revshop.cart_service.cart.model.Product;
import revshop.cart_service.cart.repository.CartItemRepository;
import revshop.cart_service.cart.repository.CartRepository;
import revshop.cart_service.cart.dto.ProductResponse;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final RestTemplate restTemplate;
    @Value("${product.service.url}")
    private String productServiceUrl;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository, RestTemplate restTemplate) {

        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.restTemplate = restTemplate;
    }

    public CartItem addItem(Long userId, Long productId, Integer quantity) {

        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Invalid quantity");
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    return cartRepository.save(newCart);
                });

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProductId(productId);
        cartItem.setQuantity(quantity);

        return cartItemRepository.save(cartItem);
    }

    public List<CartItem> getCartItems(Long userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found"));

        return cartItemRepository.findAll()
                .stream()
                .filter(item ->
                        item.getCart().getId().equals(cart.getId()))
                .toList();
    }

    public void removeItem(Long itemId) {

        if (!cartItemRepository.existsById(itemId)) {
            throw new RuntimeException("Cart item not found");
        }

        cartItemRepository.deleteById(itemId);
    }

    public CartItem updateItem(Long itemId, Integer quantity){
        CartItem existingItem = cartItemRepository.findById(itemId).orElseThrow(() -> new RuntimeException("Cart item not found"));

        if(quantity == null || quantity <=0){
            throw new RuntimeException("Quantity must be greater than 0");
        }
        existingItem.setQuantity(quantity);
        return cartItemRepository.save(existingItem);
    }

    public Double getCartTotal(Long userId) {

        List<CartItem> cartItems = getCartItems(userId);

        double total = 0;

        for (CartItem item : cartItems) {

            String url = productServiceUrl
                    + "/api/products/"
                    + item.getProductId();

            Product product = restTemplate.getForObject(
                    url,
                    Product.class
            );

            if (product == null) {
                throw new RuntimeException(
                        "Product not found: " + item.getProductId()
                );
            }

            total += product.getPrice() * item.getQuantity();
        }

        return total;
    }

    public void clearCart(Long userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found"));

        List<CartItem> cartItems = cartItemRepository.findAll()
                .stream()
                .filter(item ->
                        item.getCart().getId().equals(cart.getId()))
                .toList();

        cartItemRepository.deleteAll(cartItems);
    }

}