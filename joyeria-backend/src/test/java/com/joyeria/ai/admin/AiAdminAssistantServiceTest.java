package com.joyeria.ai.admin;

import com.joyeria.ai.AiUnavailableException;
import com.joyeria.ai.admin.client.AiAdminChatClient;
import com.joyeria.ai.admin.dto.AiAdminAction;
import com.joyeria.ai.admin.dto.AiAdminResponse;
import com.joyeria.ai.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAdminAssistantServiceTest {

    @Mock
    private AdminInsightsService insightsService;

    @Mock
    private AiAdminPromptService promptService;

    @Mock
    private AiAdminChatClient aiAdminChatClient;

    private AiProperties properties;
    private AiAdminAssistantService service;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        service = new AiAdminAssistantService(properties, insightsService, promptService, aiAdminChatClient);
    }

    private AdminInsights insights() {
        return new AdminInsights(
                120L,
                new BigDecimal("15000000"),
                new BigDecimal("1200000"),
                new BigDecimal("4500000"),
                new BigDecimal("8000000"),
                45L,
                300L,
                7L,
                4L,
                2L,
                3L,
                1L,
                Map.of("PENDING", 7L, "PAID", 20L),
                List.of(new AdminInsights.TopProduct("Anillo Aurora", 12)),
                List.of(new AdminInsights.LowStockProduct("Collar Luna", 2)),
                List.of(new AdminInsights.RecentOrder(5L, "ORD-5", "PENDING", new BigDecimal("250000"))));
    }

    private void stubProvider(String content, AiAdminPromptService.AiAdminReply reply) {
        when(aiAdminChatClient.isEnabled()).thenReturn(true);
        when(insightsService.gather()).thenReturn(insights());
        when(promptService.systemPrompt()).thenReturn("system");
        when(promptService.userPrompt(any(), any(), any())).thenReturn("user");
        when(aiAdminChatClient.chat(any(), any())).thenReturn(Optional.of(content));
        when(promptService.parse(any())).thenReturn(reply);
    }

    @Test
    void deterministicReplyWhenProviderDisabled() {
        when(aiAdminChatClient.isEnabled()).thenReturn(false);
        when(insightsService.gather()).thenReturn(insights());

        AiAdminResponse response = service.assist("¿Cómo van las ventas de hoy?");

        assertEquals("RULES", response.getGeneratedBy());
        assertTrue(response.getMessage().contains("hoy"));
        assertFalse(response.getSuggestedPrompts().isEmpty());
        assertNull(response.getAction());
    }

    @Test
    void deterministicReplyListsLowStock() {
        when(aiAdminChatClient.isEnabled()).thenReturn(false);
        when(insightsService.gather()).thenReturn(insights());

        AiAdminResponse response = service.assist("¿Qué productos tienen stock bajo?");

        assertEquals("RULES", response.getGeneratedBy());
        assertTrue(response.getMessage().contains("stock bajo"));
        assertTrue(response.getMessage().contains("Collar Luna"));
    }

    @Test
    void deterministicReplyListsPendingReviews() {
        when(aiAdminChatClient.isEnabled()).thenReturn(false);
        when(insightsService.gather()).thenReturn(insights());

        AiAdminResponse response = service.assist("¿Cuántas reseñas están pendientes de moderar?");

        assertEquals("RULES", response.getGeneratedBy());
        assertTrue(response.getMessage().contains("reseñas pendientes"));
        assertTrue(response.getMessage().contains("4"));
    }

    @Test
    void deterministicReplyListsOpenTickets() {
        when(aiAdminChatClient.isEnabled()).thenReturn(false);
        when(insightsService.gather()).thenReturn(insights());

        AiAdminResponse response = service.assist("¿Cuántos tickets de soporte hay abiertos?");

        assertEquals("RULES", response.getGeneratedBy());
        assertTrue(response.getMessage().contains("tickets de soporte"));
        assertTrue(response.getMessage().contains("2"));
    }

    @Test
    void deterministicReplyIncludesWeekRevenue() {
        when(aiAdminChatClient.isEnabled()).thenReturn(false);
        when(insightsService.gather()).thenReturn(insights());

        AiAdminResponse response = service.assist("¿Cómo van las ventas de la semana?");

        assertEquals("RULES", response.getGeneratedBy());
        assertTrue(response.getMessage().contains("7 días"));
    }

    @Test
    void aiReplyIsReturnedWithGeneratedBy() {
        stubProvider("{}", new AiAdminPromptService.AiAdminReply(
                "Las ventas de hoy suman $1.200.000.", List.of("¿Y del mes?"), null));

        AiAdminResponse response = service.assist("¿Cómo van las ventas de hoy?");

        assertEquals("AI", response.getGeneratedBy());
        assertTrue(response.getMessage().contains("1.200.000"));
        assertEquals(List.of("¿Y del mes?"), response.getSuggestedPrompts());
    }

    @Test
    void validWhitelistedActionIsReturned() {
        stubProvider("{}", new AiAdminPromptService.AiAdminReply(
                "Puedo marcar el pedido 5 como enviado.",
                List.of(),
                AiAdminAction.builder()
                        .type("update_order_status")
                        .summary("Marcar pedido 5 como SHIPPED")
                        .parameters(Map.of("orderId", "5", "status", "SHIPPED"))
                        .build()));

        AiAdminResponse response = service.assist("Marca el pedido 5 como enviado");

        assertNotNull(response.getAction());
        assertEquals("UPDATE_ORDER_STATUS", response.getAction().getType());
        assertEquals("5", response.getAction().getParameters().get("orderId"));
    }

    @Test
    void actionWithUnknownTypeIsDiscarded() {
        stubProvider("{}", new AiAdminPromptService.AiAdminReply(
                "No puedo hacer eso.",
                List.of(),
                AiAdminAction.builder()
                        .type("DELETE_EVERYTHING")
                        .parameters(Map.of())
                        .build()));

        AiAdminResponse response = service.assist("Borra toda la tienda");

        assertNull(response.getAction());
    }

    @Test
    void actionWithMissingParametersIsDiscarded() {
        stubProvider("{}", new AiAdminPromptService.AiAdminReply(
                "Faltan datos.",
                List.of(),
                AiAdminAction.builder()
                        .type("UPDATE_ORDER_STATUS")
                        .parameters(Map.of("orderId", "5"))
                        .build()));

        AiAdminResponse response = service.assist("Cambia el estado del pedido 5");

        assertNull(response.getAction());
    }

    @Test
    void providerErrorFallsBackToRules() {
        when(aiAdminChatClient.isEnabled()).thenReturn(true);
        when(insightsService.gather()).thenReturn(insights());
        when(promptService.systemPrompt()).thenReturn("system");
        when(promptService.userPrompt(any(), any(), any())).thenReturn("user");
        when(aiAdminChatClient.chat(any(), any())).thenThrow(new AiUnavailableException("caído"));

        AiAdminResponse response = service.assist("Resumen general");

        assertEquals("RULES", response.getGeneratedBy());
        assertNotNull(response.getMessage());
    }

    @Test
    void emptyMessageThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.assist("   "));
    }

    @Test
    void tooLongMessageThrows() {
        String tooLong = "a".repeat(properties.getAdmin().getMaxMessageLength() + 1);
        assertThrows(IllegalArgumentException.class, () -> service.assist(tooLong));
    }
}
