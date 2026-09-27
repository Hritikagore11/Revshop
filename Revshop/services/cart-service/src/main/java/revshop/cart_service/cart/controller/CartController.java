package revshop.cart_service.cart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import revshop.cart_service.cart.model.CartItem;
import revshop.cart_service.cart.service.CartService;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    public ResponseEntity<CartItem> addItem(
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            Authentication authentication) {

        Long userId = cartService.getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                cartService.addItem(userId, productId, quantity)
        );
    }

    @GetMapping
    public ResponseEntity<List<CartItem>> getCart(
            Authentication authentication) {

        Long userId = cartService.getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                cartService.getCartItems(userId)
        );
    }

    @GetMapping("/total")
    public ResponseEntity<Double> getCartTotal(
            @RequestHeader("Authorization") String authorizationHeader,
            Authentication authentication) {

        Long userId = cartService.getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                cartService.getCartTotal(userId, authorizationHeader)
        );
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartItem> updateItem(
            @PathVariable Long itemId,
            @RequestBody CartItem cartItem,
            Authentication authentication) {

        Long userId = cartService.getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                cartService.updateItem(itemId, cartItem.getQuantity(), userId)
        );
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long itemId,
            Authentication authentication) {

        Long userId = cartService.getAuthenticatedUserId(authentication);
        cartService.removeItem(itemId, userId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(
            Authentication authentication) {

        Long userId = cartService.getAuthenticatedUserId(authentication);
        cartService.clearCart(userId);

        return ResponseEntity.noContent().build();
    }
}
