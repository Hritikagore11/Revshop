package revshop.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import revshop.cart.model.Cart;
import revshop.cart.model.CartItem;
import revshop.cart.repository.CartItemRepository;
import revshop.cart.repository.CartRepository;
import revshop.order.model.Order;
import revshop.order.model.OrderItem;
import revshop.order.repository.OrderItemRepository;
import revshop.order.repository.OrderRepository;
import revshop.product.model.Product;
import revshop.product.repository.ProductRepository;
import revshop.user.model.User;
import revshop.user.repository.UserRepository;

import revshop.exception.CartEmptyException;
import revshop.exception.CartNotFoundException;
import revshop.exception.InsufficientStockException;
import revshop.exception.OrderNotFoundException;
import revshop.exception.UserNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Order createOrder(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        List<CartItem> cartItems = cartItemRepository.findAll()
                .stream()
                .filter(item -> item.getCart().getId().equals(cart.getId()))
                .toList();

        if (cartItems.isEmpty()) {
            throw new CartEmptyException("Cart is empty");
        }

        double totalAmount = 0;

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if (product.getQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product: " + product.getName()
                );
            }

            totalAmount += product.getPrice() * cartItem.getQuantity();
        }

        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(totalAmount);
        order.setStatus("PLACED");
        order.setCreatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {

            Product product = cartItem.getProduct();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());

            orderItemRepository.save(orderItem);

            product.setQuantity(
                    product.getQuantity() - cartItem.getQuantity()
            );

            productRepository.save(product);
        }

        cartItemRepository.deleteAll(cartItems);

        return savedOrder;
    }

    public List<Order> getOrdersByUser(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found");
        }

        return orderRepository.findByUserId(userId);
    }

    public Order getOrderById(Long orderId) {

        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    }

    public Order checkout(Long userId) {

        // Create order from user's cart
        Order order = createOrder(userId);

        return order;
    }

    public Order updateOrderStatus(Long orderId, String status) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        order.setStatus(status);

        return orderRepository.save(order);
    }

    public Order cancelOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        order.setStatus("CANCELLED");

        return orderRepository.save(order);
    }
}