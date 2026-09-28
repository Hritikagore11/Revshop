package revshop;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import revshop.cart.controller.CartController;
import revshop.cart.model.CartItem;
import revshop.cart.service.CartService;

import java.util.List;

//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Autowired(required = false)
    private ObjectMapper objectMapper;


    // 1. Test add item
    @Test
    void addItem_shouldReturnOk() throws Exception {

        CartItem cartItem = new CartItem();

        when(cartService.addItem(1L, 10L, 2))
                .thenReturn(cartItem);

        mockMvc.perform(post("/cart/items")
                        .param("userId", "1")
                        .param("productId", "10")
                        .param("quantity", "2"))
                .andExpect(status().isOk());

        verify(cartService).addItem(1L, 10L, 2);
    }


    // 2. Test get cart
    @Test
    void getCart_shouldReturnOk() throws Exception {

        CartItem item = new CartItem();

        when(cartService.getCartItems(1L))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/cart/1"))
                .andExpect(status().isOk());

        verify(cartService).getCartItems(1L);
    }


    // 3. Test get cart total
    @Test
    void getCartTotal_shouldReturnOk() throws Exception {

        when(cartService.getCartTotal(1L))
                .thenReturn(500.0);

        mockMvc.perform(get("/cart/1/total"))
                .andExpect(status().isOk())
                .andExpect(content().string("500.0"));

        verify(cartService).getCartTotal(1L);
    }


    // 4. Test update cart item
    @Test
    void updateItem_shouldReturnOk() throws Exception {

        CartItem cartItem = new CartItem();

        when(cartService.updateItem(1L, 3))
                .thenReturn(cartItem);

        mockMvc.perform(put("/cart/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "quantity": 3
                                }
                                """))
                .andExpect(status().isOk());

        verify(cartService).updateItem(1L, 3);
    }


    // 5. Test remove cart item
    @Test
    void removeItem_shouldReturnNoContent() throws Exception {

        doNothing().when(cartService).removeItem(1L);

        mockMvc.perform(delete("/cart/items/1"))
                .andExpect(status().isNoContent());

        verify(cartService).removeItem(1L);
    }


    // 6. Test clear cart
    @Test
    void clearCart_shouldReturnNoContent() throws Exception {

        doNothing().when(cartService).clearCart(1L);

        mockMvc.perform(delete("/cart/1/clear"))
                .andExpect(status().isNoContent());

        verify(cartService).clearCart(1L);
    }
}