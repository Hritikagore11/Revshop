package revshop.cart_service.cart.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import revshop.cart_service.cart.model.Cart;
import revshop.cart_service.cart.model.CartItem;
import revshop.cart_service.cart.service.CartService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private Authentication authentication;

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders
                .standaloneSetup(cartController)
                .build();

        authentication =
                new UsernamePasswordAuthenticationToken(
                        "test@gmail.com",
                        null
                );

        when(cartService.getAuthenticatedUserId(authentication))
                .thenReturn(1L);
    }

    @Test
    void addItem_shouldReturnOk() throws Exception {

        CartItem item =
                new CartItem(
                        1L,
                        new Cart(1L, 1L),
                        2L,
                        2
                );

        when(cartService.addItem(1L, 2L, 2))
                .thenReturn(item);

        mockMvc.perform(
                        post("/cart/items")
                                .param("productId", "2")
                                .param("quantity", "2")
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productId").value(2))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void getCart_shouldReturnOk() throws Exception {

        CartItem item =
                new CartItem(
                        1L,
                        new Cart(1L, 1L),
                        2L,
                        2
                );

        when(cartService.getCartItems(1L))
                .thenReturn(List.of(item));

        mockMvc.perform(
                        get("/cart")
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].productId").value(2))
                .andExpect(jsonPath("$[0].quantity").value(2));
    }

    @Test
    void getCartTotal_shouldReturnOk() throws Exception {

        when(cartService.getCartTotal(
                eq(1L),
                eq("Bearer test-token")
        )).thenReturn(360000.0);

        mockMvc.perform(
                        get("/cart/total")
                                .header(
                                        "Authorization",
                                        "Bearer test-token"
                                )
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("360000.0"));
    }

    @Test
    void updateItem_shouldReturnOk() throws Exception {

        CartItem item =
                new CartItem(
                        1L,
                        new Cart(1L, 1L),
                        2L,
                        3
                );

        when(cartService.updateQuantity(
                1L,
                1L,
                3
        )).thenReturn(item);

        mockMvc.perform(
                        put("/cart/items/1")
                                .param("quantity", "3")
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productId").value(2))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    void removeItem_shouldReturnNoContent() throws Exception {

        doNothing().when(cartService)
                .removeItem(1L, 1L);

        mockMvc.perform(
                        delete("/cart/items/1")
                                .principal(authentication)
                )
                .andExpect(status().isNoContent());

        verify(cartService)
                .removeItem(1L, 1L);
    }

    @Test
    void clearCart_shouldReturnNoContent() throws Exception {

        doNothing().when(cartService)
                .clearCart(1L);

        mockMvc.perform(
                        delete("/cart/clear")
                                .principal(authentication)
                )
                .andExpect(status().isNoContent());

        verify(cartService)
                .clearCart(1L);
    }
}