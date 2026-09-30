package revshop.order_service.order.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import revshop.order_service.client.CartClient;
import revshop.order_service.client.NotificationClient;
import revshop.order_service.client.PaymentClient;
import revshop.order_service.client.ProductClient;
import revshop.order_service.order.model.Order;
import revshop.order_service.order.model.OrderItem;
import revshop.order_service.order.repository.OrderItemRepository;
import revshop.order_service.order.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartClient cartClient;

    @Mock
    private ProductClient productClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private OrderService orderService;

    private Order order;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(
                orderService,
                "productInternalKey",
                "revshop-internal-2026"
        );

        order = new Order(
                1L,
                10L,
                5000.0,
                "PLACED",
                LocalDateTime.now()
        );
    }

    @Test
    void getAuthenticatedUserId_shouldReturnUserId() {

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "user@gmail.com",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_BUYER")
                        )
                );

        authentication.setDetails(10L);

        Long result =
                orderService.getAuthenticatedUserId(
                        authentication
                );

        assertEquals(10L, result);
    }

    @Test
    void getAuthenticatedUserId_shouldRejectMissingAuthentication() {

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> orderService
                                .getAuthenticatedUserId(null)
                );

        assertEquals(
                "User is not authenticated",
                exception.getMessage()
        );
    }

    @Test
    void createOrder_shouldCreatePlacedOrder() {

        when(orderRepository.save(any(Order.class)))
                .thenReturn(order);

        Order result =
                orderService.createOrder(
                        10L,
                        5000.0
                );

        assertNotNull(result);
        assertEquals(10L, result.getUserId());
        assertEquals(5000.0, result.getTotalAmount());
        assertEquals("PLACED", result.getStatus());
        assertNotNull(result.getCreatedAt());

        verify(orderRepository)
                .save(any(Order.class));
    }

    @Test
    void getOrdersByUser_shouldReturnOrders() {

        when(orderRepository.findByUserId(10L))
                .thenReturn(List.of(order));

        List<Order> result =
                orderService.getOrdersByUser(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(
                10L,
                result.get(0).getUserId()
        );

        verify(orderRepository)
                .findByUserId(10L);
    }

    @Test
    void getOrderById_shouldReturnOrderForOwner() {

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        Order result =
                orderService.getOrderById(
                        1L,
                        10L
                );

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(10L, result.getUserId());

        verify(orderRepository)
                .findById(1L);
    }

    @Test
    void getOrderById_shouldRejectDifferentUser() {

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> orderService.getOrderById(
                                1L,
                                99L
                        )
                );

        assertEquals(
                "You are not authorized to access this order",
                exception.getMessage()
        );
    }

    @Test
    void addOrderItem_shouldCreateItem() {

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        OrderItem item =
                new OrderItem(
                        1L,
                        order,
                        3L,
                        2,
                        2500.0
                );

        when(orderItemRepository.save(any(OrderItem.class)))
                .thenReturn(item);

        OrderItem result =
                orderService.addOrderItem(
                        1L,
                        3L,
                        2,
                        2500.0
                );

        assertNotNull(result);
        assertEquals(
                3L,
                result.getProductId()
        );
        assertEquals(
                2,
                result.getQuantity()
        );
        assertEquals(
                2500.0,
                result.getPrice()
        );
        assertEquals(
                order,
                result.getOrder()
        );

        verify(orderRepository)
                .findById(1L);

        verify(orderItemRepository)
                .save(any(OrderItem.class));
    }

    @Test
    void getOrderItems_shouldReturnItems() {

        OrderItem item =
                new OrderItem(
                        1L,
                        order,
                        3L,
                        2,
                        2500.0
                );

        when(orderItemRepository.findByOrderId(1L))
                .thenReturn(List.of(item));

        List<OrderItem> result =
                orderService.getOrderItems(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(
                3L,
                result.get(0).getProductId()
        );

        verify(orderItemRepository)
                .findByOrderId(1L);
    }

    @Test
    void updateOrderStatus_buyerShouldCancelOwnOrder() {

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderItemRepository.findByOrderId(1L))
                .thenReturn(List.of());

        when(orderRepository.save(any(Order.class)))
                .thenReturn(order);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "buyer@gmail.com",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_BUYER")
                        )
                );

        authentication.setDetails(10L);

        Order result =
                orderService.updateOrderStatus(
                        1L,
                        "CANCELLED",
                        authentication
                );

        assertEquals(
                "CANCELLED",
                result.getStatus()
        );

        verify(orderRepository)
                .save(order);
    }
}