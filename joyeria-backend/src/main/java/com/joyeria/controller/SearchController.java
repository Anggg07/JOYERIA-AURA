package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> search(
            @RequestParam String q,
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
        return ResponseEntity.ok(ApiResponse.success(
                productService.search(q, page, size, sort, direction, categoryId, materialId, minPrice, maxPrice, color, inStock)));
    }
}
