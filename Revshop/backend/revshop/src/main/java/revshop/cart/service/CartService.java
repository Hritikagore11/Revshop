package revshop.cart.service;

import org.springframework.stereotype.Service;
import revshop.cart.model.Cart;
import revshop.cart.model.CartItem;
import revshop.cart.repository.CartItemRepository;
import revshop.cart.repository.CartRepository;
import revshop.product.model.Product;
import revshop.product.repository.ProductRepository;
import revshop.user.model.User;
import revshop.user.repository.UserRepository;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public CartItem addItem(Long userId, Long productId, Integer quantity) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Invalid quantity");
        }

        if (product.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);

        return cartItemRepository.save(cartItem);
    }

    public List<CartItem> getCartItems(Long userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        return cartItemRepository.findAll()
                .stream()
                .filter(item -> item.getCart().getId().equals(cart.getId()))
                .toList();
    }

    public CartItem updateItem(Long itemId, Integer quantity) {

        CartItem existingItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        existingItem.setQuantity(quantity);

        return cartItemRepository.save(existingItem);
    }

    public void removeItem(Long itemId) {

        if (!cartItemRepository.existsById(itemId)) {
            throw new RuntimeException("Cart item not found");
        }

        cartItemRepository.deleteById(itemId);
    }
}