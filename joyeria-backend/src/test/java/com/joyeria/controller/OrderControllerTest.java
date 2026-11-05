package com.joyeria.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joyeria.dto.OrderCreateRequest;
import com.joyeria.dto.OrderDTO;
import com.joyeria.dto.PagedResponse;
import com.joyeria.exception.GlobalExceptionHandler;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.User;
import com.joyeria.model.UserRole;
import com.joyeria.service.AuthService;
import com.joyeria.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;
    @Mock
    private AuthService authService;

    @InjectMocks
    private OrderController orderController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private User user;
    private Authentication userAuth;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setRole(UserRole.USER);

        userAuth = new UsernamePasswordAuthenticationToken(
                "user@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private OrderCreateRequest validRequest() {
        return new OrderCreateRequest("Calle 1 #2-3", "Cali", "Valle", "Ana", "3001234567", "card", null, null);
    }

    @Test
    void createOrderReturnsCreatedOrder() throws Exception {
        when(authService.getCurrentUser("user@example.com")).thenReturn(user);
        OrderDTO dto = OrderDTO.builder().id(10L).orderNumber("ORD-1").build();
        when(orderService.createOrder(eq(1L), any(OrderCreateRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/orders")
                        .principal(userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Pedido creado"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-1"));
    }

    @Test
    void createOrderRejectsMissingRequiredFields() throws Exception {
        OrderCreateRequest invalid = new OrderCreateRequest(null, null, null, null, null, "card", null, null);

        mockMvc.perform(post("/api/orders")
                        .principal(userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Errores de validación"))
                .andExpect(jsonPath("$.data.shippingAddress").exists())
                .andExpect(jsonPath("$.data.shippingCity").exists());
    }

    @Test
    void getUserOrdersReturnsPage() throws Exception {
        when(authService.getCurrentUser("user@example.com")).thenReturn(user);
        PagedResponse<OrderDTO> page = PagedResponse.<OrderDTO>builder()
                .content(List.of(OrderDTO.builder().id(1L).build()))
                .page(0).size(10).totalElements(1).totalPages(1).first(true).last(true)
                .build();
        when(orderService.getUserOrders(1L, 0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/orders").principal(userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getOrderAsAdminPassesAdminFlagTrue() throws Exception {
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@example.com");
        admin.setRole(UserRole.ADMIN);
        when(authService.getCurrentUser("admin@example.com")).thenReturn(admin);
        when(orderService.getOrderByIdForUser(5L, 1L, true))
                .thenReturn(OrderDTO.builder().id(5L).build());

        mockMvc.perform(get("/api/orders/5").principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5));

        verify(orderService).getOrderByIdForUser(5L, 1L, true);
    }

    @Test
    void getOrderAsRegularUserPassesAdminFlagFalse() throws Exception {
        when(authService.getCurrentUser("user@example.com")).thenReturn(user);
        when(orderService.getOrderByIdForUser(5L, 1L, false))
                .thenReturn(OrderDTO.builder().id(5L).build());

        mockMvc.perform(get("/api/orders/5").principal(userAuth))
                .andExpect(status().isOk());

        verify(orderService).getOrderByIdForUser(5L, 1L, false);
    }

    @Test
    void getForeignOrderReturnsNotFound() throws Exception {
        when(authService.getCurrentUser("user@example.com")).thenReturn(user);
        when(orderService.getOrderByIdForUser(anyLong(), anyLong(), anyBoolean()))
                .thenThrow(new ResourceNotFoundException("Pedido no encontrado"));

        mockMvc.perform(get("/api/orders/99").principal(userAuth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Pedido no encontrado"));
    }

    @Test
    void getOrdersRespectsPaginationParams() throws Exception {
        when(authService.getCurrentUser("user@example.com")).thenReturn(user);
        when(orderService.getUserOrders(eq(1L), anyInt(), anyInt()))
                .thenReturn(PagedResponse.<OrderDTO>builder().page(2).size(5).build());

        mockMvc.perform(get("/api/orders").principal(userAuth).param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        verify(orderService).getUserOrders(1L, 2, 5);
    }
}
