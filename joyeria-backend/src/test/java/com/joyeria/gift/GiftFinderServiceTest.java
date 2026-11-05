package com.joyeria.gift;

import com.joyeria.dto.ProductDTO;
import com.joyeria.gift.dto.GiftFinderRequest;
import com.joyeria.gift.dto.GiftFinderResponse;
import com.joyeria.model.Product;
import com.joyeria.repository.MaterialRepository;
import com.joyeria.repository.ProductRepository;
import com.joyeria.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GiftFinderServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private GiftFinderService service;

    private ProductDTO product(long id, String name, String category, List<String> materials,
                               String price, int stock, String description, int soldCount) {
        return ProductDTO.builder()
                .id(id)
                .name(name)
                .slug("producto-" + id)
                .categoryName(category)
                .materialNames(materials)
                .price(new BigDecimal(price))
                .stock(stock)
                .soldCount(soldCount)
                .description(description)
                .build();
    }

    private void givenCatalog(ProductDTO... products) {
        Page<com.joyeria.model.Product> emptyPage = new PageImpl<>(List.of());
        when(productRepository.searchFiltered(any(), any(), any(), any(), any(),
                nullable(String.class), anyBoolean(), any(Pageable.class))).thenReturn(emptyPage);
        when(productService.mapProducts(anyCollection())).thenReturn(List.of(products));
    }

    private void givenNoMatches() {
        when(productRepository.searchFiltered(any(), any(), any(), any(), any(),
                nullable(String.class), anyBoolean(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(productService.mapProducts(anyCollection())).thenReturn(List.of());
    }

    @Test
    void materialCriteriaRanksMatchingProductFirst() {
        ProductDTO plata = product(1, "Anillo Luna", "Anillos", List.of("Plata"), "150000", 5, "Anillo", 0);
        ProductDTO oro = product(2, "Anillo Sol", "Anillos", List.of("Oro"), "150000", 5, "Anillo", 0);
        givenCatalog(oro, plata);

        GiftFinderResponse response = service.recommend(GiftFinderRequest.builder().material("plata").build());

        assertEquals(1L, response.getRecommendations().get(0).getId());
        assertNotNull(response.getRecommendations().get(0).getRelevance());
    }

    @Test
    void budgetInfluencesRanking() {
        ProductDTO affordable = product(1, "Collar Estela", "Collares", List.of("Oro 18K"),
                "120000", 5, "Collar elegante", 0);
        ProductDTO expensive = product(2, "Set Realeza", "Sets", List.of("Oro 18K"),
                "3200000", 5, "Set elegante", 0);
        givenCatalog(expensive, affordable);

        GiftFinderResponse response = service.recommend(GiftFinderRequest.builder()
                .budgetMax(new BigDecimal("200000")).build());

        assertEquals(1L, response.getRecommendations().get(0).getId());
        assertTrue(response.getRecommendations().get(0).getExplanation().toLowerCase().contains("presupuesto"));
    }

    @Test
    void occasionInfluencesRanking() {
        ProductDTO rings = product(1, "Anillo A", "Anillos", List.of("Oro"), "150000", 5, "Pieza", 0);
        ProductDTO charm = product(2, "Dije B", "Dijes", List.of("Oro"), "150000", 5, "Pieza", 0);
        givenCatalog(charm, rings);

        GiftFinderResponse response = service.recommend(GiftFinderRequest.builder().occasion("boda").build());

        assertEquals(1L, response.getRecommendations().get(0).getId());
    }

    @Test
    void styleInfluencesRanking() {
        ProductDTO elegant = product(1, "Collar A", "Collares", List.of("Oro"), "150000", 5, "Elegante collar", 0);
        ProductDTO plain = product(2, "Collar B", "Collares", List.of("Oro"), "150000", 5, "Collar sencillo", 0);
        givenCatalog(plain, elegant);

        GiftFinderResponse response = service.recommend(GiftFinderRequest.builder().style("elegante").build());

        assertEquals(1L, response.getRecommendations().get(0).getId());
    }

    @Test
    void incompleteFiltersStillReturnRecommendations() {
        givenCatalog(product(1, "Anillo Aurora", "Anillos", List.of("Oro"), "150000", 5, "Anillo", 0));

        GiftFinderResponse response = service.recommend(GiftFinderRequest.builder().recipient("madre").build());

        assertFalse(response.getRecommendations().isEmpty());
        assertNotNull(response.getRecommendations().get(0).getRelevance());
    }

    @Test
    void noCriteriaReturnsRecommendationsWithoutRelevance() {
        givenCatalog(product(1, "Collar Estela", "Collares", List.of("Oro"), "150000", 5, "Collar", 0));

        GiftFinderResponse response = service.recommend(GiftFinderRequest.builder().build());

        assertEquals(1, response.getRecommendations().size());
        assertNull(response.getRecommendations().get(0).getRelevance());
    }

    @Test
    void outOfStockIsFlaggedAndRanksBelowAvailable() {
        ProductDTO available = product(1, "Anillo Disponible", "Anillos", List.of("Oro"), "150000", 5, "Anillo", 0);
        ProductDTO soldOut = product(2, "Anillo Agotado", "Anillos", List.of("Oro"), "150000", 0, "Anillo", 0);
        givenCatalog(available, soldOut);

        GiftFinderResponse response = service.recommend(GiftFinderRequest.builder().build());

        assertEquals(1L, response.getRecommendations().get(0).getId());
        assertFalse(response.getRecommendations().get(1).isAvailable());
        assertTrue(response.getRecommendations().get(1).getExplanation().toLowerCase().contains("agotada"));
    }

    @Test
    void combinationWithoutMatchesReturnsEmpty() {
        givenNoMatches();

        GiftFinderResponse response = service.recommend(GiftFinderRequest.builder()
                .recipient("pareja").material("acero").budgetMax(new BigDecimal("1")).build());

        assertTrue(response.getRecommendations().isEmpty());
    }

    @Test
    void invalidBudgetRangeIsRejected() {
        GiftFinderRequest request = GiftFinderRequest.builder()
                .budgetMin(new BigDecimal("300000"))
                .budgetMax(new BigDecimal("100000"))
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.recommend(request));
    }

    @Test
    void negativeBudgetIsRejected() {
        GiftFinderRequest request = GiftFinderRequest.builder()
                .budgetMin(new BigDecimal("-1"))
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.recommend(request));
    }
}
