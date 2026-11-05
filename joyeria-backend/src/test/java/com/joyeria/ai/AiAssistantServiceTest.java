package com.joyeria.ai;

import com.joyeria.ai.client.AiChatClient;
import com.joyeria.ai.config.AiProperties;
import com.joyeria.ai.dto.AiAssistantResponse;
import com.joyeria.ai.dto.AiConversationTurn;
import com.joyeria.dto.ProductDTO;
import com.joyeria.dto.ProductImageDTO;
import com.joyeria.repository.CategoryRepository;
import com.joyeria.repository.MaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAssistantServiceTest {

    @Mock
    private ProductSearchService productSearchService;
    @Mock
    private ProductRecommendationService recommendationService;
    @Mock
    private AiChatClient aiChatClient;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private MaterialRepository materialRepository;

    private AiProperties properties;
    private AiAssistantService service;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        SearchIntentParser parser = new SearchIntentParser();
        service = new AiAssistantService(properties, new IntentClassifier(), parser,
                new ConversationContextResolver(parser), new AssistantSuggestions(), productSearchService,
                recommendationService, aiChatClient, categoryRepository, materialRepository);
    }

    private ProductDTO product(int stock) {
        return ProductDTO.builder()
                .id(1L)
                .name("Anillo Aurora")
                .slug("anillo-aurora")
                .price(new BigDecimal("185000"))
                .stock(stock)
                .soldCount(0)
                .categoryName("Anillos")
                .materialNames(List.of("Plata"))
                .images(List.of(ProductImageDTO.builder().url("u1").isPrimary(true).build(),
                        ProductImageDTO.builder().url("u2").isPrimary(false).build()))
                .build();
    }

    private ProductRecommendationService.Recommendation recommendation(ProductDTO product) {
        return new ProductRecommendationService.Recommendation(product, 40, "Recomendado para ti");
    }

    @Test
    void validQueryReturnsRealProducts() {
        ProductDTO product = product(5);
        when(aiChatClient.isEnabled()).thenReturn(false);
        when(productSearchService.findCandidates(any())).thenReturn(List.of(product));
        when(recommendationService.recommend(any(), any(), anyInt())).thenReturn(List.of(recommendation(product)));

        AiAssistantResponse response = service.assist("Busco un regalo elegante");

        assertEquals(1, response.getProducts().size());
        assertEquals("Anillo Aurora", response.getProducts().get(0).getName());
        assertEquals(new BigDecimal("185000"), response.getProducts().get(0).getPrice());
        assertTrue(response.getProducts().get(0).isAvailable());
        assertEquals("u1", response.getProducts().get(0).getImage());
        assertNotNull(response.getMessage());
    }

    @Test
    void noResultsReturnsEmptyListAndFallbackMessage() {
        when(aiChatClient.isEnabled()).thenReturn(false);
        when(productSearchService.findCandidates(any())).thenReturn(List.of());
        when(recommendationService.recommend(any(), any(), anyInt())).thenReturn(List.of());

        AiAssistantResponse response = service.assist("algo inexistente");

        assertTrue(response.getProducts().isEmpty());
        assertTrue(response.getMessage().contains("No encontré"));
        assertNotNull(response.getFollowUpQuestion());
        assertFalse(response.getSuggestions().isEmpty());
    }

    @Test
    void productResponseIncludesSuggestionsAndFollowUp() {
        when(aiChatClient.isEnabled()).thenReturn(false);
        when(productSearchService.findCandidates(any())).thenReturn(List.of(product(5)));
        when(recommendationService.recommend(any(), any(), anyInt()))
                .thenReturn(List.of(recommendation(product(5))));

        AiAssistantResponse response = service.assist("busco un anillo de plata");

        assertFalse(response.getSuggestions().isEmpty());
        assertNotNull(response.getFollowUpQuestion());
    }

    @Test
    void outOfStockProductIsMarkedUnavailable() {
        ProductDTO product = product(0);
        when(aiChatClient.isEnabled()).thenReturn(false);
        when(productSearchService.findCandidates(any())).thenReturn(List.of(product));
        when(recommendationService.recommend(any(), any(), anyInt())).thenReturn(List.of(recommendation(product)));

        AiAssistantResponse response = service.assist("un anillo");

        assertFalse(response.getProducts().get(0).isAvailable());
    }

    @Test
    void aiProviderErrorFallsBackToDeterministicResults() {
        ProductDTO product = product(5);
        when(aiChatClient.isEnabled()).thenReturn(true);
        when(categoryRepository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(materialRepository.findByActiveTrue()).thenReturn(List.of());
        when(aiChatClient.interpretIntent(any(), anyList(), anyList()))
                .thenThrow(new AiUnavailableException("proveedor caído"));
        when(aiChatClient.composeMessage(any(), any(), anyList())).thenReturn(Optional.empty());
        when(productSearchService.findCandidates(any())).thenReturn(List.of(product));
        when(recommendationService.recommend(any(), any(), anyInt())).thenReturn(List.of(recommendation(product)));

        AiAssistantResponse response = service.assist("busco un anillo de plata");

        assertEquals(1, response.getProducts().size());
        assertNotNull(response.getMessage());
    }

    @Test
    void aiTimeoutDuringComposeFallsBackToDefaultMessage() {
        ProductDTO product = product(5);
        when(aiChatClient.isEnabled()).thenReturn(true);
        when(categoryRepository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(materialRepository.findByActiveTrue()).thenReturn(List.of());
        when(aiChatClient.interpretIntent(any(), anyList(), anyList())).thenReturn(Optional.empty());
        when(aiChatClient.composeMessage(any(), any(), anyList()))
                .thenThrow(new AiUnavailableException("timeout"));
        when(productSearchService.findCandidates(any())).thenReturn(List.of(product));
        when(recommendationService.recommend(any(), any(), anyInt())).thenReturn(List.of(recommendation(product)));

        AiAssistantResponse response = service.assist("busco un anillo");

        assertEquals(1, response.getProducts().size());
        assertNotNull(response.getMessage());
        assertFalse(response.getMessage().isBlank());
    }

    @Test
    void greetingReturnsNoProductsAndDoesNotSearch() {
        AiAssistantResponse response = service.assist("Hola");

        assertEquals("GREETING", response.getIntent());
        assertTrue(response.getProducts().isEmpty());
        assertFalse(response.getMessage().isBlank());
        verifyNoInteractions(productSearchService, recommendationService, aiChatClient);
    }

    @Test
    void smallTalkReturnsNoProductsAndDoesNotSearch() {
        AiAssistantResponse response = service.assist("¿Cómo estás?");

        assertEquals("SMALL_TALK", response.getIntent());
        assertTrue(response.getProducts().isEmpty());
        verifyNoInteractions(productSearchService, recommendationService, aiChatClient);
    }

    @Test
    void thanksReturnsNoProductsAndDoesNotSearch() {
        AiAssistantResponse response = service.assist("Gracias");

        assertEquals("SMALL_TALK", response.getIntent());
        assertTrue(response.getProducts().isEmpty());
        verifyNoInteractions(productSearchService, recommendationService, aiChatClient);
    }

    @Test
    void greetingWithProductSignalStillSearches() {
        ProductDTO product = product(5);
        when(aiChatClient.isEnabled()).thenReturn(false);
        when(productSearchService.findCandidates(any())).thenReturn(List.of(product));
        when(recommendationService.recommend(any(), any(), anyInt())).thenReturn(List.of(recommendation(product)));

        AiAssistantResponse response = service.assist("hola, busco un anillo de plata");

        assertEquals("PRODUCT_SEARCH", response.getIntent());
        assertEquals(1, response.getProducts().size());
    }

    @Test
    void followUpKeepsPreviousContext() {
        when(aiChatClient.isEnabled()).thenReturn(false);
        when(productSearchService.findCandidates(any())).thenReturn(List.of(product(5)));
        when(recommendationService.recommend(any(), any(), anyInt()))
                .thenReturn(List.of(recommendation(product(5))));

        List<AiConversationTurn> history =
                List.of(new AiConversationTurn("user", "busco un anillo de plata"));

        AiAssistantResponse response = service.assist("y en oro?", history);

        SearchIntent passed = capturedIntent();
        assertEquals("anillo", passed.getCategory());
        assertEquals("oro", passed.getMaterial());
        assertEquals("PRODUCT_SEARCH", response.getIntent());
    }

    @Test
    void cheaperFollowUpLowersBudgetFromContext() {
        when(aiChatClient.isEnabled()).thenReturn(false);
        when(productSearchService.findCandidates(any())).thenReturn(List.of(product(5)));
        when(recommendationService.recommend(any(), any(), anyInt()))
                .thenReturn(List.of(recommendation(product(5))));

        List<AiConversationTurn> history =
                List.of(new AiConversationTurn("user", "un anillo por menos de 300000"));

        AiAssistantResponse response = service.assist("mas barato", history);

        assertEquals(new BigDecimal("210000"), capturedIntent().getMaxPrice());
        assertEquals("BUDGET_SEARCH", response.getIntent());
    }

    @Test
    void topicChangeDoesNotCarryPreviousFilters() {
        when(aiChatClient.isEnabled()).thenReturn(false);
        when(productSearchService.findCandidates(any())).thenReturn(List.of(product(5)));
        when(recommendationService.recommend(any(), any(), anyInt()))
                .thenReturn(List.of(recommendation(product(5))));

        List<AiConversationTurn> history =
                List.of(new AiConversationTurn("user", "busco un anillo de plata por menos de 300000"));

        service.assist("muestrame collares", history);

        SearchIntent passed = capturedIntent();
        assertEquals("collares", passed.getCategory());
        assertNull(passed.getMaterial());
        assertNull(passed.getMaxPrice());
    }

    private SearchIntent capturedIntent() {
        ArgumentCaptor<SearchIntent> captor = ArgumentCaptor.forClass(SearchIntent.class);
        verify(productSearchService, atLeastOnce()).findCandidates(captor.capture());
        return captor.getValue();
    }

    @Test
    void emptyMessageThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.assist("   "));
    }

    @Test
    void tooLongMessageThrows() {
        String tooLong = "a".repeat(properties.getMaxMessageLength() + 1);

        assertThrows(IllegalArgumentException.class, () -> service.assist(longMessage()));
    }

    private String longMessage() {
        return "a".repeat(properties.getMaxMessageLength() + 1);
    }
}
