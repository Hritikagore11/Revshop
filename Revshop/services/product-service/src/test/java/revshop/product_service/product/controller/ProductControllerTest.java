package revshop.product_service.product.controller;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import revshop.product_service.product.model.Product;
import revshop.product_service.product.service.ProductService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    private MockMvc mockMvc;

    private ProductService productService;

    private ObjectMapper objectMapper;


    @BeforeEach
    void setUp() {

        productService = mock(ProductService.class);
        objectMapper = new ObjectMapper();

        ProductController controller =
                new ProductController(productService);

        ReflectionTestUtils.setField(
                controller,
                "internalKey",
                "revshop-internal-2026"
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }


    // 1. Get all products
    @Test
    void getAllProducts_shouldReturnOk() throws Exception {

        Product product = new Product();

        product.setId(1L);
        product.setName("Laptop");
        product.setPrice(50000.0);
        product.setQuantity(10);

        when(productService.getAllProducts())
                .thenReturn(List.of(product));

        mockMvc.perform(
                        get("/api/products")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name")
                        .value("Laptop"));
    }


    // 2. Get product by ID
    @Test
    void getProductById_shouldReturnOk() throws Exception {

        Product product = new Product();

        product.setId(1L);
        product.setName("Laptop");
        product.setPrice(50000.0);
        product.setQuantity(10);

        when(productService.getProductById(1L))
                .thenReturn(product);

        mockMvc.perform(
                        get("/api/products/1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(1))
                .andExpect(jsonPath("$.name")
                        .value("Laptop"));
    }


    // 3. Search products
    @Test
    void searchProducts_shouldReturnOk() throws Exception {

        Product product = new Product();

        product.setId(1L);
        product.setName("Laptop");
        product.setPrice(50000.0);
        product.setQuantity(10);

        when(productService.searchProducts("Laptop"))
                .thenReturn(List.of(product));

        mockMvc.perform(
                        get("/api/products/search")
                                .param("keyword", "Laptop")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name")
                        .value("Laptop"));
    }


    // 4. Get products by category
    @Test
    void getProductsByCategory_shouldReturnOk() throws Exception {

        Product product = new Product();

        product.setId(1L);
        product.setName("Laptop");
        product.setPrice(50000.0);
        product.setQuantity(10);

        when(productService.getProductsByCategory("Electronics"))
                .thenReturn(List.of(product));

        mockMvc.perform(
                        get("/api/products/category")
                                .param("category", "Electronics")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name")
                        .value("Laptop"));
    }


    // 5. Create product
    @Test
    void createProduct_shouldReturnOk() throws Exception {

        Product product = new Product();

        product.setName("Laptop");
        product.setPrice(50000.0);
        product.setQuantity(10);

        Product savedProduct = new Product();

        savedProduct.setId(1L);
        savedProduct.setName("Laptop");
        savedProduct.setPrice(50000.0);
        savedProduct.setQuantity(10);
        savedProduct.setSellerId(1L);

        when(productService.createProduct(
                any(Product.class),
                eq(1L)
        ))
                .thenReturn(savedProduct);


        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "seller",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_SELLER"
                                )
                        )
                );

        /*
         * Your ProductController gets sellerId from:
         *
         * authentication.getDetails()
         *
         * Therefore we explicitly put user ID 1L
         * into authentication details.
         */
        authentication.setDetails(1L);


        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(product)
                                )
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(1))
                .andExpect(jsonPath("$.name")
                        .value("Laptop"));
    }


    // 6. Internal stock reduction
    @Test
    void reduceStockInternal_shouldReturnOk() throws Exception {

        Product product = new Product();

        product.setId(1L);
        product.setName("Laptop");
        product.setPrice(50000.0);
        product.setQuantity(8);

        when(productService.reduceStock(1L, 2))
                .thenReturn(product);


        mockMvc.perform(
                        put("/api/products/internal/1/stock")
                                .param("quantity", "2")
                                .header(
                                        "X-Internal-Key",
                                        "revshop-internal-2026"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(1))
                .andExpect(jsonPath("$.quantity")
                        .value(8));
    }
}