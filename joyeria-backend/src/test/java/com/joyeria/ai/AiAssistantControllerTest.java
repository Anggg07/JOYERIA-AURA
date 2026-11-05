package com.joyeria.ai;

import com.joyeria.ai.dto.AiAssistantResponse;
import com.joyeria.ai.dto.AiProductSuggestion;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiAssistantControllerTest {

    @Mock
    private AiAssistantService aiAssistantService;

    @InjectMocks
    private AiAssistantController controller;

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
    void validMessageReturnsMessageAndProducts() throws Exception {
        when(aiAssistantService.assist(eq("Busco un regalo"), any())).thenReturn(AiAssistantResponse.builder()
                .message("Encontré algunas opciones que podrían interesarte.")
                .products(List.of(AiProductSuggestion.builder()
                        .id(1L).name("Anillo Aurora").price(new BigDecimal("185000")).available(true).build()))
                .build());

        mockMvc.perform(post("/api/ai/assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Busco un regalo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Encontré algunas opciones que podrían interesarte."))
                .andExpect(jsonPath("$.data.products[0].name").value("Anillo Aurora"))
                .andExpect(jsonPath("$.data.products[0].price").value(185000));
    }

    @Test
    void blankMessageIsRejectedWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/ai/assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void tooLongMessageIsRejectedWithBadRequest() throws Exception {
        String longMessage = "a".repeat(501);

        mockMvc.perform(post("/api/ai/assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + longMessage + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void endpointIsPublicForUnauthenticatedUsers() throws Exception {
        when(aiAssistantService.assist(anyString(), any())).thenReturn(
                AiAssistantResponse.builder().message("ok").products(List.of()).build());

        mockMvc.perform(post("/api/ai/assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hola\"}"))
                .andExpect(status().isOk());
    }
}
