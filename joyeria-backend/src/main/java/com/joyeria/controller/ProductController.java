package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String color,
            @RequestParam(defaultValue = "false") boolean inStock) {

        if (categoryId != null || materialId != null || minPrice != null || maxPrice != null || color != null || inStock) {
            return ResponseEntity.ok(ApiResponse.success(
                    productService.getFiltered(categoryId, materialId, minPrice, maxPrice, color, inStock, page, size, sort, direction)));
        }
        return ResponseEntity.ok(ApiResponse.success(
                productService.getAllProducts(page, size, sort, direction)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductBySlug(slug)));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getFeatured() {
        return ResponseEntity.ok(ApiResponse.success(productService.getFeaturedProducts()));
    }

    @GetMapping("/new")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getNew() {
        return ResponseEntity.ok(ApiResponse.success(productService.getNewProducts()));
    }

    @GetMapping("/best-sellers")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getBestSellers() {
        return ResponseEntity.ok(ApiResponse.success(productService.getBestSellers()));
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getRelated(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getRelatedProducts(id)));
    }
}
