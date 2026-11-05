package com.joyeria.ai.admin.client;

import com.joyeria.ai.client.OpenAiChatHttp;
import com.joyeria.ai.dto.AiConversationTurn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente del asistente de administración para APIs compatibles con OpenAI.
 * Mantiene su propia URL base, clave y modelo, de modo que puede aislarse del
 * asistente de compras.
 */
@Slf4j
public class OpenAiCompatibleAdminChatClient implements AiAdminChatClient {

    private final String model;
    private final double temperature;
    private final RestClient restClient;

    public OpenAiCompatibleAdminChatClient(String baseUrl, String apiKey, String model,
                                           double temperature, int timeoutSeconds) {
        this(model, temperature, OpenAiChatHttp.buildRestClient(baseUrl, apiKey, timeoutSeconds));
    }

    /** Constructor para pruebas con un {@link RestClient} preconfigurado. */
    OpenAiCompatibleAdminChatClient(String model, double temperature, RestClient restClient) {
        this.model = model;
        this.temperature = temperature;
        this.restClient = restClient;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Optional<String> chat(String systemPrompt, List<AiConversationTurn> messages) {
        String content = OpenAiChatHttp.send(restClient, model, temperature,
                buildMessages(systemPrompt, messages));
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(content.trim());
    }

    private List<Map<String, Object>> buildMessages(String systemPrompt, List<AiConversationTurn> messages) {
        List<Map<String, Object>> payload = new ArrayList<>();
        payload.add(message("system", systemPrompt));
        if (messages != null) {
            for (AiConversationTurn turn : messages) {
                if (turn != null) {
                    payload.add(message(normalizeRole(turn.getRole()), turn.getContent()));
                }
            }
        }
        return payload;
    }

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content == null ? "" : content);
        return message;
    }

    private String normalizeRole(String role) {
        return "assistant".equalsIgnoreCase(role) ? "assistant" : "user";
    }
}
