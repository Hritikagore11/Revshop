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

import revshop.exception.*;

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
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found"));

        if (quantity == null || quantity <= 0) {
            throw new InvalidQuantityException(
                    "Quantity must be greater than 0");
        }

        if (product.getQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock");
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
                .orElseThrow(() ->
                        new CartNotFoundException("Cart not found"));

        return cartItemRepository.findByCartId(cart.getId());
    }

    public Double getCartTotal(Long userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        List<CartItem> cartItems =
                cartItemRepository.findByCartId(cart.getId());

        double total = 0;

        for (CartItem item : cartItems) {
            total += item.getProduct().getPrice() * item.getQuantity();
        }

        return total;
    }

    public CartItem updateItem(Long itemId, Integer quantity) {

        CartItem existingItem = cartItemRepository.findById(itemId)
                .orElseThrow(() ->
                        new CartItemNotFoundException("Cart item not found"));

        if (quantity == null || quantity <= 0) {
            throw new InvalidQuantityException(
                    "Quantity must be greater than 0");
        }

        existingItem.setQuantity(quantity);

        return cartItemRepository.save(existingItem);
    }

    public void removeItem(Long itemId) {

        if (!cartItemRepository.existsById(itemId)) {
            throw new CartItemNotFoundException(
                    "Cart item not found");
        }

        cartItemRepository.deleteById(itemId);
    }

    public void clearCart(Long userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new CartNotFoundException("Cart not found"));

        List<CartItem> cartItems =
                cartItemRepository.findByCartId(cart.getId());

        cartItemRepository.deleteAll(cartItems);
    }
}