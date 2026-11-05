package com.joyeria.service;

import com.joyeria.dto.CartDTO;
import com.joyeria.dto.CartItemRequest;
import com.joyeria.model.Cart;
import com.joyeria.model.CartItem;
import com.joyeria.model.Product;
import com.joyeria.model.User;
import com.joyeria.repository.CartItemRepository;
import com.joyeria.repository.CartRepository;
import com.joyeria.repository.ProductRepository;
import com.joyeria.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private Product product() {
        Product p = new Product();
        p.setId(14L);
        p.setName("Anillo de oro");
        p.setSlug("anillo-de-oro");
        p.setPrice(new BigDecimal("850000"));
        p.setStock(10);
        p.setActive(true);
        p.setImages(new ArrayList<>());
        return p;
    }

    @Test
    void addItem_OnNewCart_ReturnsCartIncludingNewItem() {
        User user = new User();
        user.setId(1L);
        Product product = product();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(14L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        Cart savedCart = new Cart();
        savedCart.setId(2L);
        savedCart.setUser(user);
        when(cartRepository.save(any(Cart.class))).thenReturn(savedCart);
        when(cartItemRepository.findByCartIdAndProductId(2L, 14L)).thenReturn(Optional.empty());

        AtomicReference<CartItem> savedItem = new AtomicReference<>();
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> {
            CartItem i = inv.getArgument(0);
            i.setId(50L);
            savedItem.set(i);
            return i;
        });
        when(cartItemRepository.findByCartId(2L)).thenAnswer(inv -> List.of(savedItem.get()));

        CartItemRequest request = new CartItemRequest(14L, 2);
        CartDTO dto = cartService.addItem(1L, request);

        assertEquals(1, dto.getItems().size());
        assertEquals(2, dto.getItemCount());
        assertEquals(new BigDecimal("1700000"), dto.getSubtotal());
        assertEquals(10, dto.getItems().get(0).getProductStock());
        assertEquals(new BigDecimal("850000"), dto.getItems().get(0).getProductPrice());
    }

    @Test
    void addItem_ExceedingStock_Throws() {
        User user = new User();
        user.setId(1L);
        Product product = product();
        product.setStock(1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(14L)).thenReturn(Optional.of(product));

        CartItemRequest request = new CartItemRequest(14L, 2);
        assertThrows(IllegalStateException.class, () -> cartService.addItem(1L, request));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_NonPositiveQuantity_Throws() {
        User user = new User();
        user.setId(1L);
        Product product = product();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(14L)).thenReturn(Optional.of(product));

        CartItemRequest request = new CartItemRequest(14L, 0);
        assertThrows(IllegalArgumentException.class, () -> cartService.addItem(1L, request));
    }
}
