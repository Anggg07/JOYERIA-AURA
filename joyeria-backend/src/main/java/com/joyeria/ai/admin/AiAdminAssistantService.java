package com.joyeria.ai.admin;

import com.joyeria.ai.AiUnavailableException;
import com.joyeria.ai.admin.client.AiAdminChatClient;
import com.joyeria.ai.admin.dto.AiAdminAction;
import com.joyeria.ai.admin.dto.AiAdminResponse;
import com.joyeria.ai.config.AiProperties;
import com.joyeria.ai.dto.AiConversationTurn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Asistente interno del panel de administración. Responde sobre el estado real
 * de la tienda (solo lectura) y puede proponer acciones que el administrador
 * debe confirmar. Nunca ejecuta cambios ni expone datos sensibles. Si el
 * proveedor de IA no está disponible, responde con reglas deterministas.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AiAdminAssistantService {

    /** Acciones que el asistente puede proponer (nunca ejecutar). */
    private static final Set<String> ALLOWED_ACTIONS =
            Set.of("UPDATE_ORDER_STATUS", "TOGGLE_USER_ACTIVE", "MODERATE_REVIEW");

    private static final Map<String, List<String>> REQUIRED_PARAMETERS = Map.of(
            "UPDATE_ORDER_STATUS", List.of("orderId", "status"),
            "TOGGLE_USER_ACTIVE", List.of("userId"),
            "MODERATE_REVIEW", List.of("reviewId", "status"));

    private static final List<String> DEFAULT_PROMPTS = List.of(
            "¿Cómo van las ventas de hoy?",
            "¿Qué pedidos están pendientes?",
            "¿Qué productos tienen stock bajo?",
            "¿Cuántas reseñas están pendientes de moderar?");

    private final AiProperties properties;
    private final AdminInsightsService insightsService;
    private final AiAdminPromptService promptService;
    private final AiAdminChatClient aiAdminChatClient;

    public AiAdminResponse assist(String rawMessage) {
        return assist(rawMessage, List.of());
    }

    public AiAdminResponse assist(String rawMessage, List<AiConversationTurn> history) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }
        int maxLength = properties.getAdmin().getMaxMessageLength();
        if (message.length() > maxLength) {
            throw new IllegalArgumentException("El mensaje es demasiado largo (máximo " + maxLength + " caracteres)");
        }

        AdminInsights insights = insightsService.gather();

        if (aiAdminChatClient.isEnabled()) {
            Optional<AiAdminResponse> aiResponse = askProvider(message, history, insights);
            if (aiResponse.isPresent()) {
                return aiResponse.get();
            }
        }

        return rulesReply(message, insights);
    }

    private Optional<AiAdminResponse> askProvider(String message, List<AiConversationTurn> history,
                                                  AdminInsights insights) {
        try {
            List<AiConversationTurn> turns = List.of(
                    new AiConversationTurn("user", promptService.userPrompt(insights, message, history)));
            Optional<String> content = aiAdminChatClient.chat(promptService.systemPrompt(), turns);
            if (content.isEmpty()) {
                return Optional.empty();
            }
            AiAdminPromptService.AiAdminReply reply = promptService.parse(content.get());
            if (reply.message() == null || reply.message().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(AiAdminResponse.builder()
                    .message(reply.message())
                    .suggestedPrompts(reply.suggestedPrompts().isEmpty() ? DEFAULT_PROMPTS : reply.suggestedPrompts())
                    .action(sanitize(reply.action()))
                    .generatedBy("AI")
                    .build());
        } catch (AiUnavailableException e) {
            log.warn("El proveedor de IA del panel no está disponible; se usa el modo determinista.");
            return Optional.empty();
        }
    }

    /**
     * Valida la acción propuesta por la IA: solo delega tipos de la lista blanca
     * y con todos los parámetros requeridos presentes. Cualquier otra cosa se
     * descarta silenciosamente.
     */
    private AiAdminAction sanitize(AiAdminAction action) {
        if (action == null || action.getType() == null) {
            return null;
        }
        String type = action.getType().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ACTIONS.contains(type)) {
            return null;
        }
        Map<String, String> parameters = action.getParameters() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(action.getParameters());
        for (String required : REQUIRED_PARAMETERS.get(type)) {
            String value = parameters.get(required);
            if (value == null || value.isBlank()) {
                return null;
            }
        }
        return AiAdminAction.builder()
                .type(type)
                .summary(action.getSummary())
                .parameters(parameters)
                .build();
    }

    private AiAdminResponse rulesReply(String message, AdminInsights insights) {
        String normalized = message.toLowerCase(Locale.ROOT);
        String body;

        if (containsAny(normalized, "ayuda", "qué puedes", "que puedes", "hola", "buenas")) {
            body = "Soy AURA Admin, tu asistente del panel. Puedo darte el estado de ventas, pedidos, "
                    + "inventario y clientes, y proponerte acciones (siempre te pido confirmación antes de aplicarlas).";
        } else if (containsAny(normalized, "stock", "inventario", "agotad", "existencias")) {
            body = "Inventario: " + orZero(insights.lowStockProducts()) + " productos con stock bajo y "
                    + orZero(insights.outOfStockProducts()) + " agotados.";
            if (!insights.lowStockList().isEmpty()) {
                body += " " + describeLowStock(insights.lowStockList()) + ".";
            }
        } else if (containsAny(normalized, "reseñ", "resena", "review", "moderac", "opinion", "opinión")) {
            body = "Hay " + orZero(insights.pendingReviews()) + " reseñas pendientes de moderación.";
        } else if (containsAny(normalized, "soporte", "ticket", "reclamo", "pqrs")) {
            body = "Hay " + orZero(insights.openTickets()) + " tickets de soporte abiertos o en proceso.";
        } else if (containsAny(normalized, "pendiente")) {
            body = "Hay " + orZero(insights.pendingOrders()) + " pedidos pendientes de un total de "
                    + orZero(insights.totalOrders()) + " pedidos registrados.";
        } else if (containsAny(normalized, "semana", "7 días", "7 dias", "ultimos dias", "últimos días")) {
            body = "Las ventas de los últimos 7 días suman " + formatCop(insights.weekRevenue()) + ".";
        } else if (containsAny(normalized, "hoy")) {
            body = "Las ventas de hoy suman " + formatCop(insights.todayRevenue()) + ".";
        } else if (containsAny(normalized, "mes")) {
            body = "Las ventas de este mes suman " + formatCop(insights.monthRevenue()) + ".";
        } else if (containsAny(normalized, "vendid", "top", "mejores")) {
            body = insights.topProducts().isEmpty()
                    ? "Aún no hay datos de productos vendidos."
                    : "Los más vendidos: " + describeTop(insights.topProducts()) + ".";
        } else if (containsAny(normalized, "cliente", "usuario")) {
            body = "La tienda tiene " + orZero(insights.totalCustomers()) + " clientes registrados.";
        } else if (containsAny(normalized, "pedido")) {
            body = "Pedidos por estado: " + describeStatuses(insights.ordersByStatus()) + ".";
        } else if (containsAny(normalized, "venta", "ingreso", "factur")) {
            body = "Las ventas totales suman " + formatCop(insights.totalRevenue()) + " en "
                    + orZero(insights.totalOrders()) + " pedidos.";
        } else {
            body = "Resumen de la tienda: ventas por " + formatCop(insights.totalRevenue()) + ", "
                    + orZero(insights.totalOrders()) + " pedidos (" + orZero(insights.pendingOrders())
                    + " pendientes), " + orZero(insights.totalProducts()) + " productos, "
                    + orZero(insights.pendingReviews()) + " reseñas por moderar y "
                    + orZero(insights.openTickets()) + " tickets de soporte abiertos.";
        }

        return AiAdminResponse.builder()
                .message(body)
                .suggestedPrompts(DEFAULT_PROMPTS)
                .action(null)
                .generatedBy("RULES")
                .build();
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String describeTop(List<AdminInsights.TopProduct> products) {
        return products.stream()
                .limit(3)
                .map(p -> p.name() + " (" + p.soldCount() + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private String describeLowStock(List<AdminInsights.LowStockProduct> products) {
        return products.stream()
                .limit(3)
                .map(p -> p.name() + " (" + p.stock() + " u.)")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private String describeStatuses(Map<String, Long> byStatus) {
        if (byStatus == null || byStatus.isEmpty()) {
            return "sin datos";
        }
        StringBuilder builder = new StringBuilder();
        byStatus.forEach((status, count) -> {
            if (count != null && count > 0) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(status).append(": ").append(count);
            }
        });
        return builder.length() == 0 ? "sin datos" : builder.toString();
    }

    private long orZero(Long value) {
        return value == null ? 0L : value;
    }

    private String formatCop(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        NumberFormat format = NumberFormat.getIntegerInstance(new Locale("es", "CO"));
        return "$" + format.format(amount);
    }
}
