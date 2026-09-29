package revshop.product_service.product.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import revshop.product_service.product.client.NotificationClient;
import revshop.product_service.product.model.Product;
import revshop.product_service.product.repository.ProductRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                productService,
                "notificationInternalKey",
                "revshop-internal-2026"
        );
    }

    // =========================================================
    // REDUCE STOCK
    // =========================================================

    @Test
    void reduceStock_shouldReduceQuantity() {

        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setQuantity(10);
        product.setLowStockThreshold(5);
        product.setPrice(50000.0);
        product.setSellerId(100L);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        Product result =
                productService.reduceStock(1L, 2);

        assertEquals(8, result.getQuantity());

        verify(productRepository)
                .save(product);

        verifyNoInteractions(notificationClient);
    }

    @Test
    void reduceStock_shouldNotifyWhenStockCrossesThreshold() {

        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setQuantity(6);
        product.setLowStockThreshold(5);
        product.setPrice(50000.0);
        product.setSellerId(100L);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        productService.reduceStock(1L, 1);

        assertEquals(5, product.getQuantity());

        verify(notificationClient)
                .createNotification(
                        any(NotificationClient.NotificationRequest.class),
                        eq("revshop-internal-2026")
                );
    }

    @Test
    void reduceStock_shouldNotNotifyWhenStockRemainsAboveThreshold() {

        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setQuantity(10);
        product.setLowStockThreshold(5);
        product.setPrice(50000.0);
        product.setSellerId(100L);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        productService.reduceStock(1L, 2);

        assertEquals(8, product.getQuantity());

        verifyNoInteractions(notificationClient);
    }

    @Test
    void reduceStock_shouldNotNotifyWhenAlreadyBelowThreshold() {

        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setQuantity(4);
        product.setLowStockThreshold(5);
        product.setPrice(50000.0);
        product.setSellerId(100L);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        productService.reduceStock(1L, 1);

        assertEquals(3, product.getQuantity());

        verifyNoInteractions(notificationClient);
    }

    @Test
    void reduceStock_shouldThrowWhenProductDoesNotExist() {

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> productService.reduceStock(999L, 1)
        );

        verify(productRepository, never())
                .save(any(Product.class));

        verifyNoInteractions(notificationClient);
    }

    @Test
    void reduceStock_shouldThrowWhenQuantityInvalid() {

        Product product = new Product();
        product.setId(1L);
        product.setQuantity(10);
        product.setLowStockThreshold(5);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> productService.reduceStock(1L, 0)
                );

        assertEquals(400, exception.getStatusCode().value());

        verify(productRepository, never())
                .save(any(Product.class));

        verifyNoInteractions(notificationClient);
    }

    @Test
    void reduceStock_shouldThrowWhenInsufficientStock() {

        Product product = new Product();
        product.setId(1L);
        product.setQuantity(3);
        product.setLowStockThreshold(5);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> productService.reduceStock(1L, 5)
                );

        assertEquals(400, exception.getStatusCode().value());

        verify(productRepository, never())
                .save(any(Product.class));

        verifyNoInteractions(notificationClient);
    }

    // =========================================================
    // IMPORTANT: NOTIFICATION FAILURE MUST NOT BREAK STOCK
    // =========================================================

    @Test
    void reduceStock_shouldStillSucceedWhenNotificationFails() {

        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setQuantity(6);
        product.setLowStockThreshold(5);
        product.setPrice(50000.0);
        product.setSellerId(100L);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        doThrow(new RuntimeException("Notification service down"))
                .when(notificationClient)
                .createNotification(
                        any(NotificationClient.NotificationRequest.class),
                        anyString()
                );

        Product result =
                productService.reduceStock(1L, 1);

        assertNotNull(result);
        assertEquals(5, result.getQuantity());

        verify(productRepository)
                .save(product);

        verify(notificationClient)
                .createNotification(
                        any(NotificationClient.NotificationRequest.class),
                        eq("revshop-internal-2026")
                );
    }

    // =========================================================
    // RESTORE STOCK
    // =========================================================

    @Test
    void restoreStock_shouldIncreaseQuantity() {

        Product product = new Product();
        product.setId(1L);
        product.setQuantity(5);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        Product result =
                productService.restoreStock(1L, 3);

        assertEquals(8, result.getQuantity());

        verify(productRepository)
                .save(product);
    }

    @Test
    void restoreStock_shouldThrowWhenProductDoesNotExist() {

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> productService.restoreStock(999L, 2)
        );

        verify(productRepository, never())
                .save(any(Product.class));
    }

    // =========================================================
    // UPDATE INVENTORY
    // =========================================================

    @Test
    void updateInventory_shouldUpdateQuantity() {

        Product product = new Product();
        product.setId(1L);
        product.setQuantity(10);
        product.setLowStockThreshold(5);
        product.setSellerId(100L);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        Product result =
                productService.updateInventory(
                        1L,
                        20,
                        100L
                );

        assertEquals(20, result.getQuantity());

        verify(productRepository)
                .save(product);
    }

    @Test
    void updateInventory_shouldRejectNegativeQuantity() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> productService.updateInventory(
                                1L,
                                -1,
                                100L
                        )
                );

        assertEquals(400, exception.getStatusCode().value());

        verifyNoInteractions(productRepository);
        verifyNoInteractions(notificationClient);
    }

    // =========================================================
    // UPDATE DISCOUNT
    // =========================================================

    @Test
    void updateDiscount_shouldUpdateDiscount() {

        Product product = new Product();
        product.setId(1L);
        product.setSellerId(100L);
        product.setDiscount(10.0);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        Product result =
                productService.updateDiscount(
                        1L,
                        25.0,
                        100L
                );

        assertEquals(25.0, result.getDiscount());

        verify(productRepository)
                .save(product);
    }

    @Test
    void updateDiscount_shouldRejectDiscountAbove100() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> productService.updateDiscount(
                                1L,
                                101.0,
                                100L
                        )
                );

        assertEquals(400, exception.getStatusCode().value());

        verifyNoInteractions(productRepository);
    }

    // =========================================================
    // OWNERSHIP
    // =========================================================

    @Test
    void updateInventory_shouldRejectDifferentSeller() {

        Product product = new Product();
        product.setId(1L);
        product.setQuantity(10);
        product.setSellerId(100L);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> productService.updateInventory(
                                1L,
                                20,
                                999L
                        )
                );

        assertEquals(403, exception.getStatusCode().value());

        verify(productRepository, never())
                .save(any(Product.class));

        verifyNoInteractions(notificationClient);
    }
}