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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Order checkout(
            Long userId,
            String paymentMethod,
            String authorizationHeader) {

        HttpHeaders headers = new HttpHeaders();

        if (authorizationHeader != null &&
                !authorizationHeader.isBlank()) {

            headers.set(
                    "Authorization",
                    authorizationHeader
            );
        }


        headers.set(
                "X-Internal-Key",
                productInternalKey
        );

        HttpEntity<Void> entity =
                new HttpEntity<>(headers);


        ResponseEntity<CartItemResponse[]> cartResponse =
                restTemplate.exchange(
                        cartServiceUrl + "/cart",
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


        Map<Long, ProductResponse> products =
                new HashMap<>();

        double totalAmount = 0.0;


        for (CartItemResponse cartItem : cartItems) {

            Long productId =
                    cartItem.getProductId();

            ResponseEntity<ProductResponse> productResponse =
                    restTemplate.exchange(
                            productServiceUrl +
                                    "/api/products/internal/" +
                                    productId,
                            HttpMethod.GET,
                            entity,
                            ProductResponse.class
                    );

            ProductResponse product =
                    productResponse.getBody();

            if (product == null) {

                throw new RuntimeException(
                        "Product not found: " +
                                productId
                );
            }

            if (cartItem.getQuantity() == null ||
                    cartItem.getQuantity() <= 0) {

                throw new RuntimeException(
                        "Invalid quantity for product: " +
                                productId
                );
            }

            if (product.getQuantity() <
                    cartItem.getQuantity()) {

                throw new RuntimeException(
                        "Insufficient stock for product: " +
                                productId
                );
            }

            products.put(productId, product);

            totalAmount +=
                    product.getPrice()
                            * cartItem.getQuantity();
        }


        Order order = new Order();

        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("PLACED");
        order.setCreatedAt(LocalDateTime.now());

        order = orderRepository.save(order);


        for (CartItemResponse cartItem : cartItems) {

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

            orderItem.setPrice(
                    product.getPrice()
            );

            orderItemRepository.save(orderItem);
        }


        List<Long> reducedProducts =
                new ArrayList<>();

        try {

            for (CartItemResponse cartItem : cartItems) {

                ProductResponse product =
                        products.get(
                                cartItem.getProductId()
                        );

                restTemplate.exchange(
                        productServiceUrl +
                                "/api/products/internal/" +
                                product.getId() +
                                "/stock?quantity=" +
                                cartItem.getQuantity(),

                        HttpMethod.PUT,

                        entity,

                        ProductResponse.class
                );

                reducedProducts.add(
                        product.getId()
                );
            }

        } catch (Exception e) {


            throw new RuntimeException(
                    "Unable to reduce product stock. " +
                            "Order was not completed.",
                    e
            );
        }


        HttpHeaders paymentHeaders =
                new HttpHeaders();

        if (authorizationHeader != null &&
                !authorizationHeader.isBlank()) {

            paymentHeaders.set(
                    "Authorization",
                    authorizationHeader
            );
        }

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

        ResponseEntity<PaymentResponse> paymentResponse;

        try {

            paymentResponse =
                    restTemplate.exchange(
                            paymentUrl,
                            HttpMethod.POST,
                            paymentEntity,
                            PaymentResponse.class
                    );

        } catch (Exception e) {

            /*
             * Payment service failed.
             */
            order.setStatus("PAYMENT_FAILED");

            orderRepository.save(order);

            throw new RuntimeException(
                    "Payment failed",
                    e
            );
        }

        if (paymentResponse.getBody() == null ||
                !"SUCCESS".equalsIgnoreCase(
                        paymentResponse
                                .getBody()
                                .getStatus())) {

            order.setStatus("PAYMENT_FAILED");

            orderRepository.save(order);

            throw new RuntimeException(
                    "Payment failed"
            );
        }

        /*
         * ------------------------------------------------
         * 7. CLEAR CART
         * ------------------------------------------------
         */

        try {

            restTemplate.exchange(
                    cartServiceUrl +
                            "/cart/clear",
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

        } catch (Exception e) {

            System.out.println(
                    "Warning: Cart could not be cleared"
            );
        }


        try {

            HttpHeaders notificationHeaders =
                    new HttpHeaders();

            if (authorizationHeader != null &&
                    !authorizationHeader.isBlank()) {

                notificationHeaders.set(
                        "Authorization",
                        authorizationHeader
                );
            }

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

        } catch (Exception e) {

            System.out.println(
                    "Warning: Notification failed: " +
                            e.getMessage()
            );
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

    @Transactional
    public Order updateOrderStatus(
            Long orderId,
            String status,
            Authentication authentication) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        Long userId = getAuthenticatedUserId(authentication);

        String role = authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "Unable to determine user role"));

        String newStatus = status.toUpperCase();

        if (!newStatus.equals("PLACED") &&
                !newStatus.equals("CONFIRMED") &&
                !newStatus.equals("SHIPPED") &&
                !newStatus.equals("DELIVERED") &&
                !newStatus.equals("CANCELLED")) {

            throw new RuntimeException(
                    "Invalid order status: " + status);
        }

        if ("BUYER".equalsIgnoreCase(role)) {

            if (!order.getUserId().equals(userId)) {

                throw new RuntimeException(
                        "You are not authorized to update this order");
            }

            if (!"CANCELLED".equals(newStatus)) {

                throw new RuntimeException(
                        "Buyer can only cancel an order");
            }

            if ("DELIVERED".equalsIgnoreCase(order.getStatus()) ||
                    "CANCELLED".equalsIgnoreCase(order.getStatus())) {

                throw new RuntimeException(
                        "This order cannot be cancelled");
            }

            // Restore stock
            restoreStockForOrder(order);

            order.setStatus("CANCELLED");

            return orderRepository.save(order);
        }


        if ("SELLER".equalsIgnoreCase(role)) {

            if ("CANCELLED".equals(newStatus)) {

                throw new RuntimeException(
                        "Seller cannot cancel an order");
            }

            String currentStatus =
                    order.getStatus().toUpperCase();

            // PLACED -> CONFIRMED
            if ("PLACED".equals(currentStatus) &&
                    !"CONFIRMED".equals(newStatus)) {

                throw new RuntimeException(
                        "PLACED order can only be CONFIRMED");
            }

            // CONFIRMED -> SHIPPED
            if ("CONFIRMED".equals(currentStatus) &&
                    !"SHIPPED".equals(newStatus)) {

                throw new RuntimeException(
                        "CONFIRMED order can only be SHIPPED");
            }

            // SHIPPED -> DELIVERED
            if ("SHIPPED".equals(currentStatus) &&
                    !"DELIVERED".equals(newStatus)) {

                throw new RuntimeException(
                        "SHIPPED order can only be DELIVERED");
            }

            // DELIVERED cannot change
            if ("DELIVERED".equals(currentStatus)) {

                throw new RuntimeException(
                        "Delivered order cannot be updated");
            }

            order.setStatus(newStatus);

            return orderRepository.save(order);
        }

        throw new RuntimeException(
                "You are not authorized to update order status");
    }

    @Transactional
    public void restoreStockForOrder(Order order) {

        List<OrderItem> orderItems =
                orderItemRepository.findByOrderId(order.getId());

        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }

        for (OrderItem item : orderItems) {

            HttpHeaders headers = new HttpHeaders();

            headers.set(
                    "X-Internal-Key",
                    productInternalKey
            );

            HttpEntity<Void> entity =
                    new HttpEntity<>(headers);

            String url =
                    productServiceUrl +
                            "/api/products/internal/" +
                            item.getProductId() +
                            "/stock/restore?quantity=" +
                            item.getQuantity();

            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    Void.class
            );
        }
    }
}