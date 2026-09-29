
        package revshop.cart_service.cart.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import revshop.cart_service.cart.client.ProductClient;
import revshop.cart_service.cart.model.Cart;
import revshop.cart_service.cart.model.CartItem;
import revshop.cart_service.cart.model.Product;
import revshop.cart_service.cart.repository.CartItemRepository;
import revshop.cart_service.cart.repository.CartRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CartService cartService;

    private Cart cart;
    private CartItem cartItem;
    private Product product;

    @BeforeEach
    void setUp() {

        // CartService uses @Value("${product.internal.key}").
        // Since this is a Mockito unit test, inject the value manually.
        ReflectionTestUtils.setField(
                cartService,
                "productInternalKey",
                "test-internal-key"
        );

        cart = new Cart(1L, 1L);

        cartItem =
                new CartItem(
                        1L,
                        cart,
                        2L,
                        2
                );

        product = new Product();
        product.setId(2L);
        product.setName("Laptop");
        product.setPrice(100000.0);
        product.setDiscount(10.0);
        product.setQuantity(10);
    }

    @Test
    void getAuthenticatedUserId_shouldReturnUserId() {

        when(authentication.getDetails())
                .thenReturn(1L);

        Long result =
                cartService.getAuthenticatedUserId(authentication);

        assertEquals(1L, result);
    }

    @Test
    void getAuthenticatedUserId_shouldThrowWhenAuthenticationMissing() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> cartService.getAuthenticatedUserId(null)
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );
    }

    @Test
    void addItem_shouldAddItemToCart() {

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository
                .findByCartIdAndProductId(1L, 2L))
                .thenReturn(Optional.empty());

        mockProductResponse(product);

        when(cartItemRepository.save(any(CartItem.class)))
                .thenReturn(cartItem);

        CartItem result =
                cartService.addItem(1L, 2L, 2);

        assertNotNull(result);
        assertEquals(2L, result.getProductId());

        verify(cartItemRepository)
                .save(any(CartItem.class));
    }

    @Test
    void addItem_shouldRejectInvalidQuantity() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> cartService.addItem(1L, 2L, 0)
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(productClient);
    }

    @Test
    void addItem_shouldRejectWhenOutOfStock() {

        Product outOfStockProduct = new Product();
        outOfStockProduct.setId(1L);
        outOfStockProduct.setQuantity(0);

        when(productClient.getProduct(
                eq(1L),
                anyString()
        )).thenReturn(outOfStockProduct);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> cartService.addItem(1L, 1L, 2)
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                "Product is out of stock",
                exception.getReason()
        );

        verify(cartRepository, never())
                .findByUserId(anyLong());

        verify(cartItemRepository, never())
                .save(any());
    }

    @Test
    void addItem_shouldRejectWhenQuantityExceedsStock() {

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository
                .findByCartIdAndProductId(1L, 2L))
                .thenReturn(Optional.empty());

        product.setQuantity(2);

        mockProductResponse(product);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> cartService.addItem(1L, 2L, 5)
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertTrue(
                exception.getReason()
                        .contains("Insufficient stock")
        );
    }

    @Test
    void getCartItems_shouldReturnItems() {

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartId(1L))
                .thenReturn(List.of(cartItem));

        List<CartItem> result =
                cartService.getCartItems(1L);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getProductId());
    }

    @Test
    void getCartItems_shouldReturnEmptyWhenCartDoesNotExist() {

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        List<CartItem> result =
                cartService.getCartItems(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getCartTotal_shouldCalculateDiscountedTotal() {

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartId(1L))
                .thenReturn(List.of(cartItem));

        mockProductResponse(product);

        Double total =
                cartService.getCartTotal(
                        1L,
                        "Bearer test-token"
                );

        // 100000 - 10% = 90000
        // quantity = 2
        // total = 180000
        assertEquals(
                180000.0,
                total
        );
    }

    @Test
    void updateQuantity_shouldUpdateItem() {

        when(cartItemRepository.findById(1L))
                .thenReturn(Optional.of(cartItem));

        mockProductResponse(product);

        when(cartItemRepository.save(cartItem))
                .thenReturn(cartItem);

        CartItem result =
                cartService.updateQuantity(
                        1L,
                        1L,
                        5
                );

        assertEquals(5, result.getQuantity());

        verify(cartItemRepository)
                .save(cartItem);
    }

    @Test
    void updateQuantity_shouldRejectQuantityGreaterThanStock() {

        when(cartItemRepository.findById(1L))
                .thenReturn(Optional.of(cartItem));

        product.setQuantity(3);

        mockProductResponse(product);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> cartService.updateQuantity(
                                1L,
                                1L,
                                5
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    @Test
    void updateQuantity_shouldRejectUnauthorizedUser() {

        Cart anotherUsersCart =
                new Cart(2L, 99L);

        CartItem anotherItem =
                new CartItem(
                        1L,
                        anotherUsersCart,
                        2L,
                        2
                );

        when(cartItemRepository.findById(1L))
                .thenReturn(Optional.of(anotherItem));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> cartService.updateQuantity(
                                1L,
                                1L,
                                2
                        )
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verifyNoInteractions(productClient);
    }

    @Test
    void removeItem_shouldDeleteOwnedItem() {

        when(cartItemRepository.findById(1L))
                .thenReturn(Optional.of(cartItem));

        cartService.removeItem(1L, 1L);

        verify(cartItemRepository)
                .delete(cartItem);
    }

    @Test
    void clearCart_shouldDeleteAllItems() {

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartId(1L))
                .thenReturn(List.of(cartItem));

        cartService.clearCart(1L);

        verify(cartItemRepository)
                .deleteAll(List.of(cartItem));
    }

    private void mockProductResponse(Product product) {

        when(productClient.getProduct(
                eq(product.getId()),
                anyString()
        )).thenReturn(product);
    }
}
