package com.joyeria.service;

import com.joyeria.dto.PagedResponse;
import com.joyeria.dto.ProductDTO;
import com.joyeria.dto.ProductImageDTO;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.*;
import com.joyeria.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public PagedResponse<ProductDTO> getUserFavorites(String email, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Page<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size));
        
        return PagedResponse.<ProductDTO>builder()
                .content(favorites.getContent().stream()
                        .map(f -> mapProductToDTO(f.getProduct()))
                        .toList())
                .page(favorites.getNumber())
                .size(favorites.getSize())
                .totalElements(favorites.getTotalElements())
                .totalPages(favorites.getTotalPages())
                .first(favorites.isFirst())
                .last(favorites.isLast())
                .build();
    }

    public boolean isFavorite(String email, Long productId) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;
        return favoriteRepository.existsByUserIdAndProductId(user.getId(), productId);
    }

    @Transactional
    public boolean toggleFavorite(String email, Long productId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        java.util.Optional<Favorite> existing = favoriteRepository.findByUserIdAndProductId(user.getId(), productId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        } else {
            Favorite favorite = new Favorite();
            favorite.setUser(user);
            favorite.setProduct(product);
            favoriteRepository.save(favorite);
            return true;
        }
    }

    private ProductDTO mapProductToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .price(product.getPrice())
                .comparePrice(product.getComparePrice())
                .sku(product.getSku())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .images(product.getImages().stream().map(img ->
                        ProductImageDTO.builder().id(img.getId()).url(img.getUrl()).alt(img.getAlt())
                                .isPrimary(img.getIsPrimary()).sortOrder(img.getSortOrder()).build()
                ).toList())
                .build();
    }
}
