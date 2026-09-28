package revshop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import revshop.cart.model.Cart;
import revshop.cart.model.CartItem;
import revshop.cart.repository.CartItemRepository;
import revshop.cart.repository.CartRepository;
import revshop.exception.*;
import revshop.order.model.Order;
import revshop.order.model.OrderItem;
import revshop.order.repository.OrderItemRepository;
import revshop.order.repository.OrderRepository;
import revshop.order.service.OrderService;
import revshop.product.model.Product;
import revshop.product.repository.ProductRepository;
import revshop.user.model.User;
import revshop.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Product product;
    private Cart cart;
    private CartItem cartItem;
    private Order order;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);

        product = new Product();
        product.setId(10L);
        product.setName("Laptop");
        product.setPrice(500.0);
        product.setQuantity(10);

        cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);

        cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);

        order = new Order();
        order.setId(1L);
        order.setUser(user);
        order.setTotalAmount(1000.0);
        order.setStatus("PLACED");
    }


    // 1. Create order successfully
    @Test
    void createOrder_success() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findAll())
                .thenReturn(List.of(cartItem));

        when(orderRepository.save(any(Order.class)))
                .thenReturn(order);

        when(orderItemRepository.save(any(OrderItem.class)))
                .thenReturn(new OrderItem());

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        Order result = orderService.createOrder(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1000.0, result.getTotalAmount());
        assertEquals("PLACED", result.getStatus());

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(productRepository).save(product);
        verify(cartItemRepository).deleteAll(List.of(cartItem));
    }


    // 2. User not found
    @Test
    void createOrder_userNotFound() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> orderService.createOrder(1L)
        );

        verify(orderRepository, never())
                .save(any(Order.class));
    }


    // 3. Cart not found
    @Test
    void createOrder_cartNotFound() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                CartNotFoundException.class,
                () -> orderService.createOrder(1L)
        );
    }


    // 4. Cart is empty
    @Test
    void createOrder_cartEmpty() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findAll())
                .thenReturn(List.of());

        assertThrows(
                CartEmptyException.class,
                () -> orderService.createOrder(1L)
        );

        verify(orderRepository, never())
                .save(any(Order.class));
    }


    // 5. Insufficient stock
    @Test
    void createOrder_insufficientStock() {

        product.setQuantity(1);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findAll())
                .thenReturn(List.of(cartItem));

        assertThrows(
                InsufficientStockException.class,
                () -> orderService.createOrder(1L)
        );

        verify(orderRepository, never())
                .save(any(Order.class));
    }


    // 6. Get orders by user successfully
    @Test
    void getOrdersByUser_success() {

        when(userRepository.existsById(1L))
                .thenReturn(true);

        when(orderRepository.findByUserId(1L))
                .thenReturn(List.of(order));

        List<Order> result =
                orderService.getOrdersByUser(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());

        verify(orderRepository)
                .findByUserId(1L);
    }


    // 7. Get orders - user not found
    @Test
    void getOrdersByUser_userNotFound() {

        when(userRepository.existsById(1L))
                .thenReturn(false);

        assertThrows(
                UserNotFoundException.class,
                () -> orderService.getOrdersByUser(1L)
        );

        verify(orderRepository, never())
                .findByUserId(1L);
    }


    // 8. Get order by ID successfully
    @Test
    void getOrderById_success() {

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        Order result =
                orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1000.0, result.getTotalAmount());

        verify(orderRepository)
                .findById(1L);
    }


    // 9. Get order - not found
    @Test
    void getOrderById_orderNotFound() {

        when(orderRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(1L)
        );
    }


    // 10. Checkout successfully
    @Test
    void checkout_success() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findAll())
                .thenReturn(List.of(cartItem));

        when(orderRepository.save(any(Order.class)))
                .thenReturn(order);

        when(orderItemRepository.save(any(OrderItem.class)))
                .thenReturn(new OrderItem());

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        Order result =
                orderService.checkout(1L);

        assertNotNull(result);
        assertEquals("PLACED", result.getStatus());

        verify(orderRepository)
                .save(any(Order.class));
    }


    // 11. Update order status successfully
    @Test
    void updateOrderStatus_success() {

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        Order result =
                orderService.updateOrderStatus(1L, "SHIPPED");

        assertEquals("SHIPPED", result.getStatus());

        verify(orderRepository)
                .save(order);
    }


    // 12. Update order status - order not found
    @Test
    void updateOrderStatus_orderNotFound() {

        when(orderRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.updateOrderStatus(1L, "SHIPPED")
        );
    }


    // 13. Cancel order successfully
    @Test
    void cancelOrder_success() {

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        Order result =
                orderService.cancelOrder(1L);

        assertEquals("CANCELLED", result.getStatus());

        verify(orderRepository)
                .save(order);
    }


    // 14. Cancel order - order not found
    @Test
    void cancelOrder_orderNotFound() {

        when(orderRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.cancelOrder(1L)
        );
    }
}