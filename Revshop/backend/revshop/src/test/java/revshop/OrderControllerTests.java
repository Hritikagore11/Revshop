package revshop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import revshop.exception.CartEmptyException;
import revshop.exception.CartNotFoundException;
import revshop.exception.OrderNotFoundException;
import revshop.exception.UserNotFoundException;
import revshop.order.controller.OrderController;
import revshop.order.model.Order;
import revshop.order.service.OrderService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;


    // 1. Create order
    @Test
    void createOrder_shouldReturnOk() throws Exception {

        Order order = new Order();

        when(orderService.createOrder(1L))
                .thenReturn(order);

        mockMvc.perform(post("/orders")
                        .param("userId", "1"))
                .andExpect(status().isOk());

        verify(orderService)
                .createOrder(1L);
    }


    // 2. Checkout
    @Test
    void checkout_shouldReturnOk() throws Exception {

        Order order = new Order();

        when(orderService.checkout(1L))
                .thenReturn(order);

        mockMvc.perform(post("/orders/checkout/1"))
                .andExpect(status().isOk());

        verify(orderService)
                .checkout(1L);
    }


    // 3. Get orders by user
    @Test
    void getOrdersByUser_shouldReturnOk() throws Exception {

        Order order = new Order();

        when(orderService.getOrdersByUser(1L))
                .thenReturn(List.of(order));

        mockMvc.perform(get("/orders/user/1"))
                .andExpect(status().isOk());

        verify(orderService)
                .getOrdersByUser(1L);
    }


    // 4. Update order status
    @Test
    void updateOrderStatus_shouldReturnOk() throws Exception {

        Order order = new Order();

        when(orderService.updateOrderStatus(1L, "SHIPPED"))
                .thenReturn(order);

        mockMvc.perform(put("/orders/1/status")
                        .param("status", "SHIPPED"))
                .andExpect(status().isOk());

        verify(orderService)
                .updateOrderStatus(1L, "SHIPPED");
    }


    // 5. Cancel order
    @Test
    void cancelOrder_shouldReturnOk() throws Exception {

        Order order = new Order();

        when(orderService.cancelOrder(1L))
                .thenReturn(order);

        mockMvc.perform(put("/orders/1/cancel"))
                .andExpect(status().isOk());

        verify(orderService)
                .cancelOrder(1L);
    }


    // 6. Get order by ID
    @Test
    void getOrderById_shouldReturnOk() throws Exception {

        Order order = new Order();

        when(orderService.getOrderById(1L))
                .thenReturn(order);

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk());

        verify(orderService)
                .getOrderById(1L);
    }


    // 7. Create order - user not found
    @Test
    void createOrder_userNotFound_shouldReturnNotFound()
            throws Exception {

        when(orderService.createOrder(1L))
                .thenThrow(
                        new UserNotFoundException("User not found")
                );

        mockMvc.perform(post("/orders")
                        .param("userId", "1"))
                .andExpect(status().isNotFound());
    }


    // 8. Checkout - cart not found
    @Test
    void checkout_cartNotFound_shouldReturnNotFound()
            throws Exception {

        when(orderService.checkout(1L))
                .thenThrow(
                        new CartNotFoundException("Cart not found")
                );

        mockMvc.perform(post("/orders/checkout/1"))
                .andExpect(status().isNotFound());
    }


    // 9. Checkout - cart empty
    @Test
    void checkout_cartEmpty_shouldReturnBadRequest()
            throws Exception {

        when(orderService.checkout(1L))
                .thenThrow(
                        new CartEmptyException("Cart is empty")
                );

        mockMvc.perform(post("/orders/checkout/1"))
                .andExpect(status().isBadRequest());
    }


    // 10. Get order - not found
    @Test
    void getOrderById_notFound_shouldReturnNotFound()
            throws Exception {

        when(orderService.getOrderById(1L))
                .thenThrow(
                        new OrderNotFoundException("Order not found")
                );

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isNotFound());
    }


    // 11. Update status - order not found
    @Test
    void updateOrderStatus_notFound_shouldReturnNotFound()
            throws Exception {

        when(orderService.updateOrderStatus(1L, "SHIPPED"))
                .thenThrow(
                        new OrderNotFoundException("Order not found")
                );

        mockMvc.perform(put("/orders/1/status")
                        .param("status", "SHIPPED"))
                .andExpect(status().isNotFound());
    }


    // 12. Cancel - order not found
    @Test
    void cancelOrder_notFound_shouldReturnNotFound()
            throws Exception {

        when(orderService.cancelOrder(1L))
                .thenThrow(
                        new OrderNotFoundException("Order not found")
                );

        mockMvc.perform(put("/orders/1/cancel"))
                .andExpect(status().isNotFound());
    }
}