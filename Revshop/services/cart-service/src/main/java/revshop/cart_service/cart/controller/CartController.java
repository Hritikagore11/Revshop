package revshop.cart_service.cart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartItem> addItem(
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            Authentication authentication) {

        Long userId =
                cartService.getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                cartService.addItem(
                        userId,
                        productId,
                        quantity
                )
        );
    }


    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<CartItem>> getCart(
            Authentication authentication) {

        Long userId =
                cartService.getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                cartService.getCartItems(userId)
        );
    }


    @GetMapping("/total")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Double> getCartTotal(
            @RequestHeader("Authorization") String authorizationHeader,
            Authentication authentication) {

        Long userId =
                cartService.getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                cartService.getCartTotal(
                        userId,
                        authorizationHeader
                )
        );
    }


    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartItem> updateItem(
            @PathVariable Long itemId,
            @RequestParam Integer quantity,
            Authentication authentication) {

        Long userId =
                cartService.getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                cartService.updateQuantity(
                        userId,
                        itemId,
                        quantity
                )
        );
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long itemId,
            Authentication authentication) {

        Long userId =
                cartService.getAuthenticatedUserId(authentication);

        cartService.removeItem(
                userId,
                itemId
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> clearCart(
            Authentication authentication) {

        Long userId =
                cartService.getAuthenticatedUserId(authentication);

        cartService.clearCart(userId);

        return ResponseEntity.noContent().build();
    }
}