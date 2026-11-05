package com.joyeria.ai.client;

import com.joyeria.ai.AiPromptService;
import com.joyeria.ai.AiUnavailableException;
import com.joyeria.ai.SearchIntent;
import com.joyeria.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente para cualquier API compatible con el endpoint <code>/chat/completions</code>
 * de OpenAI. Aísla las llamadas de red para poder sustituirlo en pruebas.
 */
@Slf4j
public class OpenAiCompatibleChatClient implements AiChatClient {

    private final AiProperties properties;
    private final AiPromptService promptService;
    private final RestClient restClient;

    public OpenAiCompatibleChatClient(AiProperties properties, AiPromptService promptService) {
        this(properties, promptService, buildRestClient(properties));
    }

    /** Constructor para pruebas con un RestClient preconfigurado. */
    OpenAiCompatibleChatClient(AiProperties properties, AiPromptService promptService, RestClient restClient) {
        this.properties = properties;
        this.promptService = promptService;
        this.restClient = restClient;
    }

    private static RestClient buildRestClient(AiProperties properties) {
        Duration timeout = Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds()));
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                .requestFactory(factory)
                .build();
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    @Override
    public boolean isEnabled() {
        return properties.isConfigured();
    }

    @Override
    public Optional<SearchIntent> interpretIntent(String message, List<String> categories, List<String> materials) {
        String content = chat(promptService.intentSystemPrompt(categories, materials), message);
        return Optional.ofNullable(promptService.parseIntentJson(content));
    }

    @Override
    public Optional<String> composeMessage(String message, SearchIntent intent, List<Map<String, Object>> products) {
        String content = chat(promptService.composeSystemPrompt(),
                promptService.composeUserPrompt(message, intent, products));
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(content.trim());
    }

    private String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("temperature", properties.getTemperature());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)));
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return extractContent(response);
        } catch (RestClientResponseException e) {
            log.warn("El proveedor de IA rechazó la solicitud: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiUnavailableException("El asistente de IA no está disponible.", e);
        } catch (RuntimeException e) {
            log.warn("Error de red o de respuesta con el proveedor de IA", e);
            throw new AiUnavailableException("El asistente de IA no está disponible.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Object choices = response.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            return null;
        }
        Object message = choice.get("message");
        if (!(message instanceof Map<?, ?> messageMap)) {
            return null;
        }
        Object content = messageMap.get("content");
        return content == null ? null : content.toString();
    }
}
