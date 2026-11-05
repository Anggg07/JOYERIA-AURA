package com.joyeria.gift;

import com.joyeria.exception.GlobalExceptionHandler;
import com.joyeria.gift.dto.GiftFinderResponse;
import com.joyeria.gift.dto.GiftRecommendation;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GiftFinderControllerTest {

    @Mock
    private GiftFinderService giftFinderService;

    @InjectMocks
    private GiftFinderController controller;

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
    void validRequestReturnsRecommendations() throws Exception {
        when(giftFinderService.recommend(any())).thenReturn(
                GiftFinderResponse.builder()
                        .recommendations(List.of(GiftRecommendation.builder()
                                .id(1L)
                                .name("Anillo Aurora")
                                .price(new BigDecimal("185000"))
                                .available(true)
                                .relevance(86)
                                .explanation("Lo recomendamos porque se ajusta a tu presupuesto.")
                                .build()))
                        .build());

        mockMvc.perform(post("/api/gift-finder/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipient\":\"pareja\",\"budgetMax\":200000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recommendations[0].name").value("Anillo Aurora"))
                .andExpect(jsonPath("$.data.recommendations[0].relevance").value(86));
    }

    @Test
    void emptyBodyIsAcceptedWithMissingCriteria() throws Exception {
        when(giftFinderService.recommend(any())).thenReturn(
                GiftFinderResponse.builder().recommendations(List.of()).build());

        mockMvc.perform(post("/api/gift-finder/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recommendations").isArray());
    }

    @Test
    void negativeBudgetIsRejectedWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/gift-finder/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budgetMin\":-10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void invalidBudgetRangeIsRejectedWithBadRequest() throws Exception {
        when(giftFinderService.recommend(any()))
                .thenThrow(new IllegalArgumentException("El presupuesto mínimo no puede ser mayor que el máximo"));

        mockMvc.perform(post("/api/gift-finder/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budgetMin\":300000,\"budgetMax\":100000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void malformedBodyIsRejectedWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/gift-finder/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budgetMax\":\"no-es-numero\"}"))
                .andExpect(status().isBadRequest());
    }
}
