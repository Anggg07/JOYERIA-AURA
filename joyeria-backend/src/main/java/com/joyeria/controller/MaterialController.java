package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MaterialDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(materialService.getAllMaterials()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(materialService.getMaterialById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MaterialDTO>> create(@Valid @RequestBody MaterialCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Material creado", materialService.createMaterial(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MaterialDTO>> update(
            @PathVariable Long id, @Valid @RequestBody MaterialCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Material actualizado", materialService.updateMaterial(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return ResponseEntity.ok(ApiResponse.success("Material eliminado", null));
    }
}
