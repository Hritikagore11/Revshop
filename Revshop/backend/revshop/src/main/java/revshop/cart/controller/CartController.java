package revshop.cart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import revshop.cart.model.CartItem;
import revshop.cart.service.CartService;

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
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {

        return ResponseEntity.ok(
                cartService.addItem(userId, productId, quantity)
        );
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<CartItem>> getCart(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                cartService.getCartItems(userId)
        );
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartItem> updateItem(
            @PathVariable Long itemId,
            @RequestBody CartItem cartItem){

        CartItem updatedItem = cartService.updateItem(
                itemId,
                cartItem.getQuantity()
        );

        return ResponseEntity.ok(updatedItem);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long itemId) {

        cartService.removeItem(itemId);

        return ResponseEntity.noContent().build();
    }

}