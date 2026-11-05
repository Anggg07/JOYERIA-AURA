package com.joyeria.service;

import com.joyeria.dto.PagedResponse;
import com.joyeria.dto.ProductDTO;
import com.joyeria.model.Product;
import com.joyeria.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Anillo Aurora");
        product.setSlug("anillo-aurora");
        product.setPrice(new BigDecimal("1850000"));
        product.setStock(10);
        product.setActive(true);
        product.setSoldCount(0);
        product.setFeatured(false);
        product.setIsNew(true);
        product.setBestSeller(false);
    }

    private void stubListing() {
        when(productRepository.findByActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1));
        when(reviewRepository.aggregateRatings(any(), any())).thenReturn(List.of());
    }

    @Test
    void getAllProducts_WithDefaultParams_ReturnsPage() {
        stubListing();

        PagedResponse<ProductDTO> response = productService.getAllProducts(0, 20, "createdAt", "desc");

        assertEquals(1, response.getContent().size());
        assertEquals("Anillo Aurora", response.getContent().get(0).getName());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findByActiveTrue(captor.capture());
        assertEquals(20, captor.getValue().getPageSize());
        assertEquals("createdAt", captor.getValue().getSort().getOrderFor("createdAt").getProperty());
        assertTrue(captor.getValue().getSort().getOrderFor("createdAt").isDescending());
    }

    @Test
    void getAllProducts_WithSizeAboveMax_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.getAllProducts(0, 5000, "createdAt", "desc"));
        assertThrows(IllegalArgumentException.class,
                () -> productService.getAllProducts(0, 0, "createdAt", "desc"));
        verifyNoInteractions(productRepository);
    }

    @Test
    void getAllProducts_WithNegativePage_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.getAllProducts(-1, 20, "createdAt", "desc"));
        verifyNoInteractions(productRepository);
    }

    @Test
    void getAllProducts_WithUnknownSort_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.getAllProducts(0, 20, "password", "desc"));
        assertThrows(IllegalArgumentException.class,
                () -> productService.getAllProducts(0, 20, "price; DROP TABLE products", "asc"));
        verifyNoInteractions(productRepository);
    }

    @Test
    void getAllProducts_WithInvalidDirection_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.getAllProducts(0, 20, "price", "sideways"));
        verifyNoInteractions(productRepository);
    }

    @Test
    void search_UsesValidatedPageable() {
        when(productRepository.searchFiltered(any(), any(), any(), any(), any(), any(), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 12), 1));
        when(reviewRepository.aggregateRatings(any(), any())).thenReturn(List.of());

        productService.search("anillo", 0, 12, "price", "asc",
                null, null, null, null, null, false);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).searchFiltered(any(), any(), any(), any(), any(), any(), anyBoolean(), captor.capture());
        assertTrue(captor.getValue().getSort().getOrderFor("price").isAscending());
    }

    @Test
    void search_WithUnknownSort_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.search("q", 0, 12, "unknownField", "asc",
                        null, null, null, null, null, false));
        verifyNoInteractions(productRepository);
    }
}
