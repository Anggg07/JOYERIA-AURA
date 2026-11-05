package com.joyeria.ai.client;

import com.joyeria.ai.AiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Utilidades compartidas para llamar a cualquier API compatible con el endpoint
 * {@code /chat/completions} de OpenAI. Aísla la construcción del cliente HTTP y
 * la extracción de la respuesta para reutilizarla entre el asistente de compras
 * y el asistente interno del panel de administración.
 */
@Slf4j
public final class OpenAiChatHttp {

    private OpenAiChatHttp() {
    }

    /** Construye un {@link RestClient} autenticado con el timeout indicado. */
    public static RestClient buildRestClient(String baseUrl, String apiKey, int timeoutSeconds) {
        Duration timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /**
     * Envía una conversación al proveedor y devuelve el contenido del primer
     * mensaje de la respuesta. Lanza {@link AiUnavailableException} ante cualquier
     * fallo de red o del proveedor.
     */
    public static String send(RestClient restClient, String model, double temperature,
                              List<Map<String, Object>> messages) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("messages", messages);
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {
                    });
            return extractContent(response);
        } catch (RestClientResponseException e) {
            log.warn("El proveedor de IA respondió con error: {}", e.getStatusCode());
            throw new AiUnavailableException("El proveedor de IA no está disponible.", e);
        } catch (RuntimeException e) {
            log.warn("No se pudo contactar al proveedor de IA: {}", e.getMessage());
            throw new AiUnavailableException("El proveedor de IA no está disponible.", e);
        }
    }

    /** Extrae el contenido del primer "choice" de una respuesta de chat completions. */
    public static String extractContent(Map<String, Object> response) {
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

    public static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
