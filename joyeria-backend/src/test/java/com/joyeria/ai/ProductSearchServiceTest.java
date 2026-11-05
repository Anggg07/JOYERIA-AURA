package com.joyeria.ai;

import com.joyeria.model.Product;
import com.joyeria.repository.CategoryRepository;
import com.joyeria.repository.MaterialRepository;
import com.joyeria.repository.ProductRepository;
import com.joyeria.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductSearchService service;

    @Captor
    private ArgumentCaptor<List<Product>> poolCaptor;

    private Product product(long id) {
        Product product = new Product();
        product.setId(id);
        return product;
    }

    private PageImpl<Product> page(Product... products) {
        return new PageImpl<>(List.of(products));
    }

    @Test
    void mergesKeywordMatchesWithStructuredResultsWithoutDuplicates() {
        Product structuredOnly = product(1);
        Product shared = product(2);
        Product keywordOnly = product(3);

        when(productRepository.searchFiltered(eq(""), any(), any(), any(), any(), any(), anyBoolean(),
                any(Pageable.class))).thenReturn(page(structuredOnly, shared));
        when(productRepository.searchFiltered(eq("grabado"), any(), any(), any(), any(), any(), anyBoolean(),
                any(Pageable.class))).thenReturn(page(shared, keywordOnly));
        when(productService.mapProducts(anyCollection())).thenReturn(List.of());

        SearchIntent intent = SearchIntent.builder()
                .keywords(new LinkedHashSet<>(Set.of("grabado")))
                .build();

        service.findCandidates(intent);

        verify(productService).mapProducts(poolCaptor.capture());
        List<Long> ids = poolCaptor.getValue().stream().map(Product::getId).toList();
        assertEquals(List.of(1L, 2L, 3L), ids);
    }

    @Test
    void structuredOnlySearchRunsASingleQuery() {
        when(productRepository.searchFiltered(eq(""), any(), any(), any(), any(), any(), anyBoolean(),
                any(Pageable.class))).thenReturn(page(product(1)));
        when(productService.mapProducts(anyCollection())).thenReturn(List.of());

        service.findCandidates(SearchIntent.builder().build());

        verify(productRepository, times(1)).searchFiltered(any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(Pageable.class));
    }

    @Test
    void noMatchesReturnsEmptyPool() {
        when(productRepository.searchFiltered(any(), any(), any(), any(), any(), any(), anyBoolean(),
                any(Pageable.class))).thenReturn(page());
        when(productService.mapProducts(anyCollection())).thenReturn(List.of());

        SearchIntent intent = SearchIntent.builder()
                .keywords(new LinkedHashSet<>(Set.of("inexistente")))
                .build();

        assertTrue(service.findCandidates(intent).isEmpty());
    }
}
