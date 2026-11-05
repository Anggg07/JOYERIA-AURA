package com.joyeria.ai.admin;

import com.joyeria.ai.admin.dto.AiAdminAction;
import com.joyeria.ai.admin.dto.AiAdminResponse;
import com.joyeria.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiAdminControllerTest {

    @Mock
    private AiAdminAssistantService aiAdminAssistantService;

    @InjectMocks
    private AiAdminController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void returnsMessageSuggestionsAndAction() throws Exception {
        when(aiAdminAssistantService.assist(any(), any())).thenReturn(AiAdminResponse.builder()
                .message("Puedo marcar el pedido 5 como enviado.")
                .suggestedPrompts(List.of("¿Y del mes?"))
                .action(AiAdminAction.builder()
                        .type("UPDATE_ORDER_STATUS")
                        .summary("Marcar pedido 5 como SHIPPED")
                        .parameters(Map.of("orderId", "5", "status", "SHIPPED"))
                        .build())
                .generatedBy("AI")
                .build());

        mockMvc.perform(post("/api/admin/ai/assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Marca el pedido 5 como enviado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Puedo marcar el pedido 5 como enviado."))
                .andExpect(jsonPath("$.data.action.type").value("UPDATE_ORDER_STATUS"))
                .andExpect(jsonPath("$.data.generatedBy").value("AI"));
    }

    @Test
    void blankMessageIsRejectedWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/ai/assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void tooLongMessageIsRejectedWithBadRequest() throws Exception {
        String longMessage = "a".repeat(601);

        mockMvc.perform(post("/api/admin/ai/assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + longMessage + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
