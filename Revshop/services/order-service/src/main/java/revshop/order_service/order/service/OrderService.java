package revshop.order_service.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import revshop.order_service.order.dto.CartItemResponse;
import revshop.order_service.order.dto.PaymentResponse;
import revshop.order_service.order.dto.ProductResponse;
import revshop.order_service.order.model.Order;
import revshop.order_service.order.model.OrderItem;
import revshop.order_service.order.repository.OrderItemRepository;
import revshop.order_service.order.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestTemplate restTemplate;

    @Value("${cart.service.url}")
    private String cartServiceUrl;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    @Value("${product.internal.key}")
    private String productInternalKey;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            RestTemplate restTemplate) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.restTemplate = restTemplate;
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

    @Transactional
    public Order checkout(
            Long userId,
            String paymentMethod,
            String authorizationHeader) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);
        headers.set("X-Internal-Key", productInternalKey);

        HttpEntity<Void> entity =
                new HttpEntity<>(headers);

        ResponseEntity<CartItemResponse[]> cartResponse =
                restTemplate.exchange(
                        cartServiceUrl +
                                "/cart",
                        HttpMethod.GET,
                        entity,
                        CartItemResponse[].class
                );

        CartItemResponse[] cartItems =
                cartResponse.getBody();

        if (cartItems == null ||
                cartItems.length == 0) {

            throw new RuntimeException("Cart is empty");
        }

        double totalAmount = 0.0;

        /*
         * First pass:
         * Validate products and calculate the
         * actual total from product-service.
         */
        for (CartItemResponse cartItem : cartItems) {

            ResponseEntity<ProductResponse> productResponse =
                    restTemplate.exchange(
                            productServiceUrl +
                                    "/api/products/internal/" +
                                    cartItem.getProductId(),
                            HttpMethod.GET,
                            entity,
                            ProductResponse.class
                    );

            ProductResponse product =
                    productResponse.getBody();

            if (product == null) {

                throw new RuntimeException(
                        "Product not found: " +
                                cartItem.getProductId()
                );
            }

            if (product.getQuantity() <
                    cartItem.getQuantity()) {

                throw new RuntimeException(
                        "Insufficient stock for product: " +
                                product.getId()
                );
            }

            double price = product.getPrice();

            totalAmount +=
                    price * cartItem.getQuantity();
        }

        /*
         * Create order using server-calculated
         * total amount.
         */
        Order order = new Order();

        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("PLACED");
        order.setCreatedAt(LocalDateTime.now());

        order = orderRepository.save(order);

        /*
         * Create order items and reduce stock.
         */
        for (CartItemResponse cartItem : cartItems) {

            ResponseEntity<ProductResponse> productResponse =
                    restTemplate.exchange(
                            productServiceUrl +
                                    "/api/products/internal/" +
                                    cartItem.getProductId(),
                            HttpMethod.GET,
                            entity,
                            ProductResponse.class
                    );

            ProductResponse product =
                    productResponse.getBody();

            if (product == null) {

                throw new RuntimeException(
                        "Product not found: " +
                                cartItem.getProductId()
                );
            }

            double price = product.getPrice();

            OrderItem orderItem =
                    new OrderItem();

            orderItem.setOrder(order);
            orderItem.setProductId(
                    cartItem.getProductId()
            );
            orderItem.setQuantity(
                    cartItem.getQuantity()
            );
            orderItem.setPrice(price);

            orderItemRepository.save(orderItem);

            /*
             * Reduce product stock.
             */
            HttpHeaders stockHeaders =
                    new HttpHeaders();

            stockHeaders.set(
                    "Authorization",
                    authorizationHeader
            );

            stockHeaders.set(
                    "X-Internal-Key",
                    productInternalKey
            );

            HttpEntity<Void> stockEntity =
                    new HttpEntity<>(stockHeaders);

            restTemplate.exchange(
                    productServiceUrl +
                            "/api/products/internal/" +
                            product.getId() +
                            "/stock?quantity=" +
                            cartItem.getQuantity(),
                    HttpMethod.PUT,
                    stockEntity,
                    Void.class
            );
        }

        /*
         * Process payment using the server-calculated
         * order total.
         */
        HttpHeaders paymentHeaders =
                new HttpHeaders();

        paymentHeaders.set(
                "Authorization",
                authorizationHeader
        );

        HttpEntity<Void> paymentEntity =
                new HttpEntity<>(paymentHeaders);

        String paymentUrl =
                paymentServiceUrl +
                        "/payments?orderId=" +
                        order.getId() +
                        "&paymentMethod=" +
                        paymentMethod +
                        "&amount=" +
                        totalAmount;

        ResponseEntity<PaymentResponse> paymentResponse =
                restTemplate.exchange(
                        paymentUrl,
                        HttpMethod.POST,
                        paymentEntity,
                        PaymentResponse.class
                );

        if (paymentResponse.getBody() == null ||
                !"SUCCESS".equalsIgnoreCase(
                        paymentResponse.getBody().getStatus())) {

            throw new RuntimeException(
                    "Payment failed"
            );
        }

        /*
         * Clear cart after successful payment.
         */
        restTemplate.exchange(
                cartServiceUrl +
                        "/cart/clear",
                HttpMethod.DELETE,
                entity,
                Void.class
        );

        /*
         * Send notification.
         */
        try {

            HttpHeaders notificationHeaders =
                    new HttpHeaders();

            notificationHeaders.set(
                    "Authorization",
                    authorizationHeader
            );

            notificationHeaders.setContentType(
                    MediaType.APPLICATION_JSON
            );

            String notificationJson =
                    """
                    {
                        "userId": %d,
                        "message": "Order #%d placed successfully",
                        "type": "ORDER",
                        "isRead": false
                    }
                    """.formatted(
                            userId,
                            order.getId()
                    );

            HttpEntity<String> notificationEntity =
                    new HttpEntity<>(
                            notificationJson,
                            notificationHeaders
                    );

            restTemplate.exchange(
                    notificationServiceUrl +
                            "/notifications",
                    HttpMethod.POST,
                    notificationEntity,
                    Void.class
            );

        } catch (Exception ignored) {
            /*
             * Notification failure should not
             * invalidate an already successful order.
             */
        }

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

        String role = authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Unable to determine user role"));

        /*
         * Seller can update order status.
         */
        if ("SELLER".equalsIgnoreCase(role)) {

            order.setStatus(status);

            return orderRepository.save(order);
        }

        /*
         * Buyer can only update their own order.
         */
        Long userId =
                getAuthenticatedUserId(authentication);

        if (!order.getUserId()
                .equals(userId)) {

            throw new RuntimeException(
                    "You are not authorized to update this order"
            );
        }

        order.setStatus(status);

        return orderRepository.save(order);
    }
}