package revshop.cart_service.cart.service;

import org.springframework.stereotype.Service;
import revshop.cart_service.cart.model.Cart;
import revshop.cart_service.cart.model.CartItem;
import revshop.cart_service.cart.repository.CartItemRepository;
import revshop.cart_service.cart.repository.CartRepository;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository) {

        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
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
}