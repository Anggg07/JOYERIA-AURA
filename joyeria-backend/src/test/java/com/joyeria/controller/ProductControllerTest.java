package com.joyeria.controller;

import com.joyeria.dto.PagedResponse;
import com.joyeria.dto.ProductDTO;
import com.joyeria.exception.GlobalExceptionHandler;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void listUsesPlainEndpointWithoutFilters() throws Exception {
        when(productService.getAllProducts(0, 20, "createdAt", "desc"))
                .thenReturn(PagedResponse.<ProductDTO>builder().page(0).size(20).build());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        verify(productService).getAllProducts(0, 20, "createdAt", "desc");
    }

    @Test
    void listRoutesToFilteredEndpointWhenFilterPresent() throws Exception {
        when(productService.getFiltered(anyLong(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(PagedResponse.<ProductDTO>builder().page(0).size(20).build());

        mockMvc.perform(get("/api/products").param("categoryId", "5").param("minPrice", "50000"))
                .andExpect(status().isOk());

        verify(productService).getFiltered(
                org.mockito.ArgumentMatchers.eq(5L), any(), any(BigDecimal.class), any(), any(), anyBoolean(),
                org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(20), anyString(), anyString());
    }

    @Test
    void getProductByIdReturnsDto() throws Exception {
        when(productService.getProductById(7L)).thenReturn(ProductDTO.builder().id(7L).name("Anillo").price(new BigDecimal("150000")).build());

        mockMvc.perform(get("/api/products/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.name").value("Anillo"));
    }

    @Test
    void getMissingProductReturnsNotFound() throws Exception {
        when(productService.getProductById(anyLong())).thenThrow(new ResourceNotFoundException("Producto no encontrado"));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Producto no encontrado"));
    }

    @Test
    void featuredReturnsList() throws Exception {
        when(productService.getFeaturedProducts()).thenReturn(List.of(ProductDTO.builder().id(1L).build()));

        mockMvc.perform(get("/api/products/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void getRelatedReturnsList() throws Exception {
        when(productService.getRelatedProducts(1L)).thenReturn(List.of(ProductDTO.builder().id(2L).build()));

        mockMvc.perform(get("/api/products/1/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(2));
    }
}