package revshop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import revshop.exception.*;
import revshop.cart.model.Cart;
import revshop.cart.model.CartItem;
import revshop.cart.repository.CartItemRepository;
import revshop.cart.repository.CartRepository;
import revshop.cart.service.CartService;
import revshop.product.model.Product;
import revshop.product.repository.ProductRepository;
import revshop.user.model.User;
import revshop.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Product product;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);

        product = new Product();
        product.setId(1L);
        product.setPrice(100.0);
        product.setQuantity(10);

        cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);

        cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
    }


    // 1
    @Test
    void addItem_success() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.save(any(CartItem.class)))
                .thenReturn(cartItem);

        CartItem result =
                cartService.addItem(1L, 1L, 2);

        assertNotNull(result);
        assertEquals(2, result.getQuantity());

        verify(cartItemRepository).save(any(CartItem.class));
    }


    // 2
    @Test
    void addItem_userNotFound() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> cartService.addItem(1L, 1L, 2)
        );
    }


    // 3
    @Test
    void addItem_productNotFound() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> cartService.addItem(1L, 1L, 2)
        );
    }


    // 4
    @Test
    void addItem_invalidQuantity() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        assertThrows(
                InvalidQuantityException.class,
                () -> cartService.addItem(1L, 1L, 0)
        );
    }


    // 5
    @Test
    void addItem_insufficientStock() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        assertThrows(
                InsufficientStockException.class,
                () -> cartService.addItem(1L, 1L, 20)
        );
    }


    // 6
    @Test
    void getCartItems_success() {

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartId(1L))
                .thenReturn(List.of(cartItem));

        List<CartItem> result =
                cartService.getCartItems(1L);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getQuantity());
    }


    // 7
    @Test
    void getCartItems_cartNotFound() {

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                CartNotFoundException.class,
                () -> cartService.getCartItems(1L)
        );
    }


    // 8
    @Test
    void getCartTotal_success() {

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartId(1L))
                .thenReturn(List.of(cartItem));

        Double total =
                cartService.getCartTotal(1L);

        assertEquals(200.0, total);
    }


    // 9
    @Test
    void updateItem_success() {

        when(cartItemRepository.findById(1L))
                .thenReturn(Optional.of(cartItem));

        when(cartItemRepository.save(cartItem))
                .thenReturn(cartItem);

        CartItem result =
                cartService.updateItem(1L, 5);

        assertEquals(5, result.getQuantity());

        verify(cartItemRepository).save(cartItem);
    }


    // 10
    @Test
    void updateItem_invalidQuantity() {

        when(cartItemRepository.findById(1L))
                .thenReturn(Optional.of(cartItem));

        assertThrows(
                InvalidQuantityException.class,
                () -> cartService.updateItem(1L, 0)
        );
    }


    // 11
    @Test
    void updateItem_itemNotFound() {

        when(cartItemRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                CartItemNotFoundException.class,
                () -> cartService.updateItem(1L, 5)
        );
    }


    // 12
    @Test
    void removeItem_success() {

        when(cartItemRepository.existsById(1L))
                .thenReturn(true);

        cartService.removeItem(1L);

        verify(cartItemRepository)
                .deleteById(1L);
    }


    // 13
    @Test
    void removeItem_itemNotFound() {

        when(cartItemRepository.existsById(1L))
                .thenReturn(false);

        assertThrows(
                CartItemNotFoundException.class,
                () -> cartService.removeItem(1L)
        );

        verify(cartItemRepository, never())
                .deleteById(1L);
    }


    // 14
    @Test
    void clearCart_success() {

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartId(1L))
                .thenReturn(List.of(cartItem));

        cartService.clearCart(1L);

        verify(cartItemRepository)
                .deleteAll(List.of(cartItem));
    }


    // 15
    @Test
    void clearCart_cartNotFound() {

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                CartNotFoundException.class,
                () -> cartService.clearCart(1L)
        );

        verify(cartItemRepository, never())
                .deleteAll(anyList());
    }
}