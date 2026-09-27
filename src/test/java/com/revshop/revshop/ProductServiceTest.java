package com.revshop.revshop;

import com.revshop.revshop.model.Product;
import com.revshop.revshop.repository.ProductRepository;
import com.revshop.revshop.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void addProduct_shouldSaveProduct() {

        Product product = new Product();
        product.setName("Laptop");
        product.setPrice(50000.0);

        when(productRepository.save(product)).thenReturn(product);

        Product result = productService.addProduct(product);

        assertEquals("Laptop", result.getName());
        assertEquals(50000.0, result.getPrice());

        verify(productRepository).save(product);
    }

    @Test
    void getProductById_shouldReturnProduct() {

        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        Optional<Product> result = productService.getProductById(1L);

        assertTrue(result.isPresent());
        assertEquals("Laptop", result.get().getName());

        verify(productRepository).findById(1L);
    }

    @Test
    void deleteProduct_shouldDeleteExistingProduct() {

        when(productRepository.existsById(1L))
                .thenReturn(true);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void reduceStock_shouldReduceQuantity() {

        Product product = new Product();
        product.setId(1L);
        product.setQuantity(10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product))
                .thenReturn(product);

        Product result = productService.reduceStock(1L, 3);

        assertEquals(7, result.getQuantity());

        verify(productRepository).save(product);
    }

    @Test
    void discountedPrice_shouldCalculateCorrectly() {

        Product product = new Product();

        product.setPrice(50000.0);
        product.setDiscount(10.0);

        assertEquals(45000.0, product.getDiscountedPrice());
    }
}