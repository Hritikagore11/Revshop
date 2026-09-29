package revshop.order_service.order.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import revshop.order_service.order.dto.CheckoutRequest;
import revshop.order_service.order.model.Order;
import revshop.order_service.order.model.OrderItem;
import revshop.order_service.order.service.OrderService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    private UsernamePasswordAuthenticationToken buyerAuthentication;

    private Order order;

    @BeforeEach
    void setUp() {

        OrderController controller =
                new OrderController(orderService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        buyerAuthentication =
                new UsernamePasswordAuthenticationToken(
                        "buyer@gmail.com",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_BUYER"
                                )
                        )
                );

        buyerAuthentication.setDetails(10L);

        order = new Order(
                1L,
                10L,
                5000.0,
                "PLACED",
                LocalDateTime.now()
        );
    }

    @Test
    void createOrder_shouldReturnOk() throws Exception {

        when(orderService.getAuthenticatedUserId(any()))
                .thenReturn(10L);

        when(orderService.createOrder(10L, 5000.0))
                .thenReturn(order);

        mockMvc.perform(
                        post("/orders")
                                .param("totalAmount", "5000")
                                .principal(buyerAuthentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.status").value("PLACED"));

        verify(orderService).createOrder(10L, 5000.0);
    }

    @Test
    void getOrdersByUser_shouldReturnOkForOwner() throws Exception {

        when(orderService.getAuthenticatedUserId(any()))
                .thenReturn(10L);

        when(orderService.getOrdersByUser(10L))
                .thenReturn(List.of(order));

        mockMvc.perform(
                        get("/orders/user/10")
                                .principal(buyerAuthentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(10));

        verify(orderService).getOrdersByUser(10L);
    }

    @Test
    void getOrdersByUser_shouldReturnForbiddenForDifferentUser()
            throws Exception {

        when(orderService.getAuthenticatedUserId(any()))
                .thenReturn(10L);

        mockMvc.perform(
                        get("/orders/user/99")
                                .principal(buyerAuthentication)
                )
                .andExpect(status().isForbidden());

        verify(orderService, never())
                .getOrdersByUser(anyLong());
    }

    @Test
    void getOrderById_shouldReturnOk() throws Exception {

        when(orderService.getAuthenticatedUserId(any()))
                .thenReturn(10L);

        when(orderService.getOrderById(1L, 10L))
                .thenReturn(order);

        mockMvc.perform(
                        get("/orders/1")
                                .principal(buyerAuthentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.totalAmount").value(5000.0));

        verify(orderService)
                .getOrderById(1L, 10L);
    }

    @Test
    void getOrderItems_shouldReturnOk() throws Exception {

        OrderItem item =
                new OrderItem(
                        1L,
                        order,
                        3L,
                        2,
                        2500.0
                );

        when(orderService.getAuthenticatedUserId(any()))
                .thenReturn(10L);

        when(orderService.getOrderById(1L, 10L))
                .thenReturn(order);

        when(orderService.getOrderItems(1L))
                .thenReturn(List.of(item));

        mockMvc.perform(
                        get("/orders/1/items")
                                .principal(buyerAuthentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(3))
                .andExpect(jsonPath("$[0].quantity").value(2));

        verify(orderService).getOrderItems(1L);
    }

    @Test
    void getOrderHistory_shouldReturnOk() throws Exception {

        when(orderService.getAuthenticatedUserId(any()))
                .thenReturn(10L);

        when(orderService.getOrdersByUser(10L))
                .thenReturn(List.of(order));

        mockMvc.perform(
                        get("/orders/history")
                                .principal(buyerAuthentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("PLACED"));

        verify(orderService).getOrdersByUser(10L);
    }
}