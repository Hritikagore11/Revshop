package revshop.cart_service.cart.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import revshop.cart_service.cart.model.Cart;
import revshop.cart_service.cart.model.CartItem;
import revshop.cart_service.cart.model.Product;
import revshop.cart_service.cart.repository.CartItemRepository;
import revshop.cart_service.cart.repository.CartRepository;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${product.internal.key}")
    private String productInternalKey;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            RestTemplate restTemplate) {

        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.restTemplate = restTemplate;
    }

    public Long getAuthenticatedUserId(
            Authentication authentication) {

        if (authentication == null ||
                authentication.getDetails() == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User is not authenticated"
            );
        }

        Object details = authentication.getDetails();

        if (!(details instanceof Long userId)) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unable to identify authenticated user"
            );
        }

        return userId;
    }

    @Transactional
    public CartItem addItem(
            Long userId,
            Long productId,
            Integer quantity) {

        if (productId == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product ID is required"
            );
        }

        if (quantity == null || quantity <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be greater than zero"
            );
        }

        Product product = getProduct(productId);

        if (product.getQuantity() == null ||
                product.getQuantity() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product is out of stock"
            );
        }

        Cart cart =
                cartRepository.findByUserId(userId)
                        .orElseGet(() -> {

                            Cart newCart = new Cart();
                            newCart.setUserId(userId);

                            return cartRepository.save(newCart);
                        });

        CartItem cartItem =
                cartItemRepository
                        .findByCartIdAndProductId(
                                cart.getId(),
                                productId
                        )
                        .orElseGet(CartItem::new);

        int existingQuantity =
                cartItem.getId() == null
                        ? 0
                        : cartItem.getQuantity();

        int newQuantity =
                existingQuantity + quantity;

        if (newQuantity > product.getQuantity()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient stock. Available stock: "
                            + product.getQuantity()
            );
        }

        cartItem.setCart(cart);
        cartItem.setProductId(productId);
        cartItem.setQuantity(newQuantity);

        return cartItemRepository.save(cartItem);
    }

    public List<CartItem> getCartItems(Long userId) {

        return cartRepository
                .findByUserId(userId)
                .map(cart ->
                        cartItemRepository
                                .findByCartId(cart.getId())
                )
                .orElseGet(List::of);
    }

    public Double getCartTotal(
            Long userId,
            String authorizationHeader) {

        List<CartItem> cartItems =
                getCartItems(userId);

        double total = 0.0;

        for (CartItem item : cartItems) {

            Product product =
                    getProduct(
                            item.getProductId(),
                            authorizationHeader
                    );

            if (product.getPrice() == null) {
                continue;
            }

            double price =
                    product.getPrice();

            double discount =
                    product.getDiscount() == null
                            ? 0.0
                            : product.getDiscount();

            double finalPrice =
                    price -
                            (price * discount / 100.0);

            total +=
                    finalPrice * item.getQuantity();
        }

        return total;
    }

    @Transactional
    public CartItem updateQuantity(
            Long userId,
            Long itemId,
            Integer quantity) {

        if (quantity == null || quantity <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be greater than zero"
            );
        }

        CartItem item =
                getOwnedCartItem(
                        itemId,
                        userId
                );

        Product product =
                getProduct(
                        item.getProductId()
                );

        if (product.getQuantity() == null ||
                product.getQuantity() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product is out of stock"
            );
        }

        if (quantity > product.getQuantity()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient stock. Available stock: "
                            + product.getQuantity()
            );
        }

        item.setQuantity(quantity);

        return cartItemRepository.save(item);
    }

    @Transactional
    public void removeItem(
            Long userId,
            Long itemId) {

        CartItem item =
                getOwnedCartItem(
                        itemId,
                        userId
                );

        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(Long userId) {

        cartRepository
                .findByUserId(userId)
                .ifPresent(cart -> {

                    List<CartItem> items =
                            cartItemRepository
                                    .findByCartId(
                                            cart.getId()
                                    );

                    cartItemRepository.deleteAll(items);
                });
    }

    private CartItem getOwnedCartItem(
            Long itemId,
            Long userId) {

        CartItem item =
                cartItemRepository
                        .findById(itemId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Cart item not found"
                                ));

        if (item.getCart() == null ||
                !userId.equals(
                        item.getCart().getUserId()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to modify this cart item"
            );
        }

        return item;
    }

    private Product getProduct(
            Long productId) {

        return getProduct(
                productId,
                null
        );
    }

    private Product getProduct(
            Long productId,
            String authorizationHeader) {

        String url =
                productServiceUrl +
                        "/api/products/internal/" +
                        productId;

        HttpHeaders headers =
                new HttpHeaders();

        headers.set(
                "X-Internal-Key",
                productInternalKey
        );

        if (authorizationHeader != null &&
                !authorizationHeader.isBlank()) {

            headers.set(
                    "Authorization",
                    authorizationHeader
            );
        }

        HttpEntity<Void> entity =
                new HttpEntity<>(headers);

        try {

            ResponseEntity<Product> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            Product.class
                    );

            Product product =
                    response.getBody();

            if (product == null) {

                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found: " +
                                productId
                );
            }

            return product;

        } catch (ResponseStatusException e) {

            throw e;

        } catch (RestClientException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to fetch product: " +
                            productId
            );
        }
    }
}