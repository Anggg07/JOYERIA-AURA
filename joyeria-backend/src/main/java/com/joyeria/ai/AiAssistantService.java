package com.joyeria.ai;

import com.joyeria.ai.client.AiChatClient;
import com.joyeria.ai.config.AiProperties;
import com.joyeria.ai.dto.AiAssistantResponse;
import com.joyeria.ai.dto.AiConversationTurn;
import com.joyeria.ai.dto.AiProductSuggestion;
import com.joyeria.dto.ProductDTO;
import com.joyeria.model.Category;
import com.joyeria.model.Material;
import com.joyeria.repository.CategoryRepository;
import com.joyeria.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orquesta el asistente: valida la entrada, interpreta la intención, consulta
 * productos reales, ordena por relevancia y, opcionalmente, deja que la IA
 * redacte el mensaje. Cualquier fallo de IA se degrada a modo determinista sin
 * romper la tienda.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AiAssistantService {

    private final AiProperties properties;
    private final IntentClassifier intentClassifier;
    private final SearchIntentParser intentParser;
    private final ConversationContextResolver conversationContextResolver;
    private final AssistantSuggestions assistantSuggestions;
    private final ProductSearchService productSearchService;
    private final ProductRecommendationService recommendationService;
    private final AiChatClient aiChatClient;
    private final CategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;

    public AiAssistantResponse assist(String rawMessage) {
        return assist(rawMessage, List.of());
    }

    public AiAssistantResponse assist(String rawMessage, List<AiConversationTurn> history) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }
        if (message.length() > properties.getMaxMessageLength()) {
            throw new IllegalArgumentException(
                    "El mensaje es demasiado largo (máximo " + properties.getMaxMessageLength() + " caracteres)");
        }

        SearchIntent baseIntent = intentParser.parse(message);
        AiIntent baseAiIntent = intentClassifier.classify(message, baseIntent);

        if (intentClassifier.isConversational(baseAiIntent)) {
            return AiAssistantResponse.builder()
                    .intent(baseAiIntent.name())
                    .message(conversationalMessage(baseAiIntent, message))
                    .products(List.of())
                    .followUpQuestion(assistantSuggestions.followUpQuestion(baseAiIntent, baseIntent, false))
                    .suggestions(assistantSuggestions.suggestions(baseAiIntent, baseIntent, false))
                    .build();
        }

        SearchIntent intent = conversationContextResolver.enrich(baseIntent, message, history);
        AiIntent aiIntent = intentClassifier.classify(message, intent);

        List<ProductDTO> candidates = productSearchService.findCandidates(intent);

        if (aiChatClient.isEnabled()) {
            intent = refineWithAi(message, intent);
            candidates = productSearchService.findCandidates(intent);
        }

        List<ProductRecommendationService.Recommendation> recommendations =
                recommendationService.recommend(candidates, intent, properties.getMaxProducts());

        List<AiProductSuggestion> suggestions = recommendations.stream()
                .map(this::toSuggestion)
                .toList();

        String responseMessage = resolveMessage(message, intent, recommendations);
        boolean hasProducts = !recommendations.isEmpty();
        return AiAssistantResponse.builder()
                .intent(aiIntent.name())
                .message(responseMessage)
                .products(suggestions)
                .followUpQuestion(assistantSuggestions.followUpQuestion(aiIntent, intent, hasProducts))
                .suggestions(assistantSuggestions.suggestions(aiIntent, intent, hasProducts))
                .build();
    }

    /**
     * Respuesta para intenciones puramente conversacionales. Nunca consulta el
     * catálogo ni muestra tarjetas.
     */
    private String conversationalMessage(AiIntent intent, String message) {
        return switch (intent) {
            case GREETING -> "¡Hola! 😊 Soy AURA, tu asistente de joyería. "
                    + "¿En qué puedo ayudarte hoy? Si buscas un regalo o una joya especial, cuéntame.";
            case SMALL_TALK -> intentClassifier.isThankYou(message)
                    ? "¡Con gusto! 💛 Me alegra poder ayudarte. "
                            + "¿Quieres que te muestre alguna joya o tienes otra pregunta?"
                    : "¡Muy bien, gracias por preguntar! 😊 Estoy aquí para ayudarte a encontrar la joya perfecta. "
                            + "¿Buscas algo en particular?";
            case CATALOG_QUESTION -> "¡Claro! 💛 Puedo ayudarte a explorar anillos, collares, pulseras, aretes, "
                    + "dijes y sets, en oro, plata y otros materiales. ¿Qué tipo de joya te interesa?";
            case OUT_OF_SCOPE -> "Puedo ayudarte sobre todo con joyería 💛. "
                    + "¿Buscas un anillo, un regalo, algo en plata…? Cuéntame y te muestro opciones reales.";
            default -> "¿En qué puedo ayudarte?";
        };
    }

    private SearchIntent refineWithAi(String message, SearchIntent deterministic) {
        try {
            List<String> categories = categoryNames();
            List<String> materials = materialNames();
            Optional<SearchIntent> aiIntent = aiChatClient.interpretIntent(message, categories, materials);
            if (aiIntent.isEmpty()) {
                return deterministic;
            }
            Set<String> knownCategories = categories.stream()
                    .map(TextNormalizer::normalize).collect(Collectors.toSet());
            Set<String> knownMaterials = materials.stream()
                    .map(TextNormalizer::normalize).collect(Collectors.toSet());
            return intentParser.merge(deterministic, aiIntent.get(), knownCategories, knownMaterials);
        } catch (AiUnavailableException e) {
            log.warn("El proveedor de IA no está disponible; se usa la interpretación determinista.");
            return deterministic;
        }
    }

    private String resolveMessage(String message, SearchIntent intent,
                                  List<ProductRecommendationService.Recommendation> recommendations) {
        if (aiChatClient.isEnabled() && properties.isComposeResponse() && !recommendations.isEmpty()) {
            try {
                List<Map<String, Object>> payload = recommendations.stream()
                        .map(recommendation -> productPayload(recommendation.product()))
                        .toList();
                Optional<String> composed = aiChatClient.composeMessage(message, intent, payload);
                if (composed.isPresent() && !composed.get().isBlank()) {
                    return composed.get();
                }
            } catch (AiUnavailableException e) {
                log.warn("No se pudo redactar la respuesta con IA; se usa el mensaje por defecto.");
            }
        }
        return defaultMessage(recommendations);
    }

    private String defaultMessage(List<ProductRecommendationService.Recommendation> recommendations) {
        if (recommendations.isEmpty()) {
            return "No encontré productos que coincidan con tu búsqueda. Prueba con otras palabras o explora nuestro catálogo completo.";
        }
        return "Encontré estas opciones que podrían interesarte.";
    }

    private Map<String, Object> productPayload(ProductDTO product) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", product.getId());
        payload.put("name", product.getName());
        payload.put("price", product.getPrice());
        payload.put("category", product.getCategoryName());
        payload.put("materials", product.getMaterialNames());
        return payload;
    }

    private AiProductSuggestion toSuggestion(ProductRecommendationService.Recommendation recommendation) {
        ProductDTO product = recommendation.product();
        return AiProductSuggestion.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .price(product.getPrice())
                .image(primaryImage(product))
                .material(product.getMaterialNames() == null || product.getMaterialNames().isEmpty()
                        ? null : String.join(", ", product.getMaterialNames()))
                .available(product.getStock() != null && product.getStock() > 0)
                .reason(recommendation.reason())
                .build();
    }

    private String primaryImage(ProductDTO product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .map(image -> image.getUrl())
                .findFirst()
                .orElseGet(() -> product.getImages().get(0).getUrl());
    }

    private List<String> categoryNames() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(Category::getName)
                .toList();
    }

    private List<String> materialNames() {
        return materialRepository.findByActiveTrue().stream()
                .map(Material::getName)
                .toList();
    }
}
