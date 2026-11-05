package com.joyeria.service;

import com.joyeria.dto.*;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.*;
import com.joyeria.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartDTO getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(userRepository.findById(userId).orElse(null));
                    return cartRepository.save(newCart);
                });
        return mapToDTO(cart);
    }

    @Transactional
    public CartDTO addItem(Long userId, CartItemRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new IllegalArgumentException("La cantidad debe ser al menos 1");
        }

        if (!product.getActive() || product.getStock() < request.getQuantity()) {
            throw new IllegalStateException("El producto no está disponible o no tiene suficiente stock");
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });

        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> {
                    CartItem item = new CartItem();
                    item.setCart(cart);
                    item.setProduct(product);
                    item.setQuantity(0);
                    return item;
                });

        int newQuantity = cartItem.getQuantity() + request.getQuantity();
        if (product.getStock() < newQuantity) {
            throw new IllegalStateException("Solo hay " + product.getStock() + " unidad(es) disponible(s) de " + product.getName());
        }

        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);

        return mapToDTO(cart);
    }

    @Transactional
    public CartDTO updateItemQuantity(Long userId, Long itemId, Integer quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("El item no pertenece a este carrito");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            Product product = item.getProduct();
            if (product != null && product.getStock() < quantity) {
                throw new IllegalStateException("Stock insuficiente para: " + product.getName());
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return mapToDTO(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Transactional
    public CartDTO removeItem(Long userId, Long itemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("El item no pertenece a este carrito");
        }

        cartItemRepository.delete(item);
        return mapToDTO(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private CartDTO mapToDTO(Cart cart) {
        List<CartItemDTO> items = cartItemRepository.findByCartId(cart.getId()).stream()
                .map(item -> CartItemDTO.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .productSlug(item.getProduct().getSlug())
                        .productPrice(item.getProduct().getPrice())
                        .productStock(item.getProduct().getStock())
                        .productImage(item.getProduct().getImages().isEmpty() ?
                                null : item.getProduct().getImages().get(0).getUrl())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build()
                ).toList();

        BigDecimal subtotal = items.stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartDTO.builder()
                .id(cart.getId())
                .items(items)
                .itemCount(items.stream().mapToInt(CartItemDTO::getQuantity).sum())
                .subtotal(subtotal)
                .build();
    }
}
