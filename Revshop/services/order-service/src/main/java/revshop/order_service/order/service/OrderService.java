package revshop.order_service.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import revshop.order_service.client.CartClient;
import revshop.order_service.client.NotificationClient;
import revshop.order_service.client.PaymentClient;
import revshop.order_service.client.ProductClient;
import revshop.order_service.order.dto.CartItemResponse;
import revshop.order_service.order.dto.PaymentResponse;
import revshop.order_service.order.dto.ProductResponse;
import revshop.order_service.order.model.Order;
import revshop.order_service.order.model.OrderItem;
import revshop.order_service.order.repository.OrderItemRepository;
import revshop.order_service.order.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartClient cartClient;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;
    private final NotificationClient notificationClient;

    @Value("${product.internal.key}")
    private String productInternalKey;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CartClient cartClient,
            ProductClient productClient,
            PaymentClient paymentClient,
            NotificationClient notificationClient) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartClient = cartClient;
        this.productClient = productClient;
        this.paymentClient = paymentClient;
        this.notificationClient = notificationClient;
    }

    public Long getAuthenticatedUserId(Authentication authentication) {

        if (authentication == null ||
                authentication.getPrincipal() == null) {

            throw new RuntimeException("User is not authenticated");
        }

        Object details = authentication.getDetails();

        if (details instanceof Long userId) {
            return userId;
        }

        throw new RuntimeException(
                "Unable to identify authenticated user"
        );
    }

    public Order createOrder(
            Long userId,
            Double totalAmount) {

        Order order = new Order();

        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("PLACED");
        order.setCreatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    public Order checkout(
            Long userId,
            String paymentMethod,
            String authorizationHeader) {

        // 1. Get cart using Feign
        CartItemResponse[] cartItems;

        try {

            cartItems =
                    cartClient.getCart(authorizationHeader);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to fetch cart",
                    e
            );
        }

        if (cartItems == null ||
                cartItems.length == 0) {

            throw new RuntimeException(
                    "Cart is empty"
            );
        }

        // 2. Fetch products and calculate total
        Map<Long, ProductResponse> products =
                new HashMap<>();

        double totalAmount = 0.0;

        for (CartItemResponse cartItem : cartItems) {

            Long productId =
                    cartItem.getProductId();

            ProductResponse product;

            try {

                product =
                        productClient.getProduct(
                                productId,
                                productInternalKey
                        );

            } catch (Exception e) {

                throw new RuntimeException(
                        "Unable to fetch product: " +
                                productId,
                        e
                );
            }

            if (product == null) {

                throw new RuntimeException(
                        "Product not found: " +
                                productId
                );
            }

            // Validate quantity
            if (cartItem.getQuantity() == null ||
                    cartItem.getQuantity() <= 0) {

                throw new RuntimeException(
                        "Invalid quantity for product: " +
                                productId
                );
            }

            // Validate stock
            if (product.getQuantity() == null ||
                    product.getQuantity() <
                            cartItem.getQuantity()) {

                throw new RuntimeException(
                        "Insufficient stock for product: " +
                                productId
                );
            }

            products.put(
                    productId,
                    product
            );

            // Calculate discounted price
            double price =
                    product.getPrice();

            double discount =
                    product.getDiscount() == null
                            ? 0.0
                            : product.getDiscount();

            double finalPrice =
                    price -
                            (price * discount / 100.0);

            totalAmount +=
                    finalPrice *
                            cartItem.getQuantity();
        }

        // 3. Create order
        Order order = new Order();

        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("PLACED");
        order.setCreatedAt(LocalDateTime.now());

        order =
                orderRepository.save(order);

        // 4. Create order items
        for (CartItemResponse cartItem :
                cartItems) {

            ProductResponse product =
                    products.get(
                            cartItem.getProductId()
                    );

            OrderItem orderItem =
                    new OrderItem();

            orderItem.setOrder(order);

            orderItem.setProductId(
                    cartItem.getProductId()
            );

            orderItem.setQuantity(
                    cartItem.getQuantity()
            );

            double price =
                    product.getPrice();

            double discount =
                    product.getDiscount() == null
                            ? 0.0
                            : product.getDiscount();

            double finalPrice =
                    price -
                            (price * discount / 100.0);

            orderItem.setPrice(finalPrice);

            orderItemRepository.save(
                    orderItem
            );
        }

        // 5. Reduce product stock using Feign
        try {

            for (CartItemResponse cartItem :
                    cartItems) {

                productClient.reduceStock(
                        cartItem.getProductId(),
                        cartItem.getQuantity(),
                        productInternalKey
                );
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to reduce product stock. " +
                            "Order was not completed.",
                    e
            );
        }

        // 6. Make payment using Feign
        PaymentResponse paymentResponse;

        try {

            paymentResponse =
                    paymentClient.createPayment(
                            order.getId(),
                            paymentMethod,
                            totalAmount,
                            authorizationHeader
                    );

        } catch (Exception e) {

            order.setStatus(
                    "PAYMENT_FAILED"
            );

            orderRepository.save(order);

            throw new RuntimeException(
                    "Payment failed",
                    e
            );
        }

        // 7. Validate payment response
        if (paymentResponse == null ||
                !"SUCCESS".equalsIgnoreCase(
                        paymentResponse.getStatus()
                )) {

            order.setStatus(
                    "PAYMENT_FAILED"
            );

            orderRepository.save(order);

            throw new RuntimeException(
                    "Payment failed"
            );
        }

        // 8. Clear cart using Feign
        try {

            cartClient.clearCart(
                    authorizationHeader
            );

        } catch (Exception e) {

            System.out.println(
                    "Warning: Cart could not be cleared"
            );
        }

        // 9. Send notification using Feign
        try {

            NotificationClient.NotificationRequest
                    notificationRequest =
                    new NotificationClient.NotificationRequest(
                            userId,
                            "Order #" +
                                    order.getId() +
                                    " placed successfully",
                            "ORDER",
                            false
                    );

            notificationClient.createNotification(
                    notificationRequest,
                    authorizationHeader
            );

        } catch (Exception e) {

            System.out.println(
                    "Warning: Notification failed: " +
                            e.getMessage()
            );
        }

        // 10. Return completed order
        return order;
    }

    public List<Order> getOrdersByUser(
            Long userId) {

        return orderRepository.findByUserId(userId);
    }

    public Order getOrderById(
            Long orderId,
            Long authenticatedUserId) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                ));

        if (!order.getUserId()
                .equals(authenticatedUserId)) {

            throw new RuntimeException(
                    "You are not authorized to access this order"
            );
        }

        return order;
    }

    public OrderItem addOrderItem(
            Long orderId,
            Long productId,
            Integer quantity,
            Double price) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                ));

        OrderItem item =
                new OrderItem();

        item.setOrder(order);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setPrice(price);

        return orderItemRepository.save(item);
    }

    public List<OrderItem> getOrderItems(
            Long orderId) {

        return orderItemRepository
                .findByOrderId(orderId);
    }

    @Transactional
    public Order updateOrderStatus(
            Long orderId,
            String status,
            Authentication authentication) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                ));

        Long userId =
                getAuthenticatedUserId(authentication);

        String role =
                authentication.getAuthorities()
                        .stream()
                        .map(authority ->
                                authority.getAuthority())
                        .filter(authority ->
                                authority.startsWith("ROLE_"))
                        .map(authority ->
                                authority.substring(5))
                        .findFirst()
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Unable to determine user role"
                                ));

        String newStatus =
                status.toUpperCase();

        if (!newStatus.equals("PLACED") &&
                !newStatus.equals("CONFIRMED") &&
                !newStatus.equals("SHIPPED") &&
                !newStatus.equals("DELIVERED") &&
                !newStatus.equals("CANCELLED")) {

            throw new RuntimeException(
                    "Invalid order status: " + status
            );
        }

        if ("BUYER".equalsIgnoreCase(role)) {

            if (!order.getUserId().equals(userId)) {

                throw new RuntimeException(
                        "You are not authorized to update this order"
                );
            }

            if (!"CANCELLED".equals(newStatus)) {

                throw new RuntimeException(
                        "Buyer can only cancel an order"
                );
            }

            if ("DELIVERED".equalsIgnoreCase(order.getStatus()) ||
                    "CANCELLED".equalsIgnoreCase(order.getStatus())) {

                throw new RuntimeException(
                        "This order cannot be cancelled"
                );
            }

            restoreStockForOrder(order);

            order.setStatus("CANCELLED");

            return orderRepository.save(order);
        }

        if ("SELLER".equalsIgnoreCase(role)) {

            if ("CANCELLED".equals(newStatus)) {

                throw new RuntimeException(
                        "Seller cannot cancel an order"
                );
            }

            String currentStatus =
                    order.getStatus().toUpperCase();

            if ("PLACED".equals(currentStatus) &&
                    !"CONFIRMED".equals(newStatus)) {

                throw new RuntimeException(
                        "PLACED order can only be CONFIRMED"
                );
            }

            if ("CONFIRMED".equals(currentStatus) &&
                    !"SHIPPED".equals(newStatus)) {

                throw new RuntimeException(
                        "CONFIRMED order can only be SHIPPED"
                );
            }

            if ("SHIPPED".equals(currentStatus) &&
                    !"DELIVERED".equals(newStatus)) {

                throw new RuntimeException(
                        "SHIPPED order can only be DELIVERED"
                );
            }

            if ("DELIVERED".equals(currentStatus)) {

                throw new RuntimeException(
                        "Delivered order cannot be updated"
                );
            }

            order.setStatus(newStatus);

            return orderRepository.save(order);
        }

        throw new RuntimeException(
                "You are not authorized to update order status"
        );
    }

    @Transactional
    public void restoreStockForOrder(Order order) {

        List<OrderItem> orderItems =
                orderItemRepository.findByOrderId(order.getId());

        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }

        for (OrderItem item : orderItems) {

            try {

                productClient.restoreStock(
                        item.getProductId(),
                        item.getQuantity(),
                        productInternalKey
                );

            } catch (Exception e) {

                throw new RuntimeException(
                        "Unable to restore stock for product: " +
                                item.getProductId(),
                        e
                );
            }
        }
    }
}