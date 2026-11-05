package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.service.SocialLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/social-links")
@RequiredArgsConstructor
public class SocialLinkController {

    private final SocialLinkService socialLinkService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SocialLinkDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(socialLinkService.getAllLinks()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SocialLinkDTO>>> getAllAdmin() {
        return ResponseEntity.ok(ApiResponse.success(socialLinkService.getAllLinksAdmin()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SocialLinkDTO>> create(@Valid @RequestBody SocialLinkCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Enlace creado", socialLinkService.createLink(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SocialLinkDTO>> update(
            @PathVariable Long id, @Valid @RequestBody SocialLinkCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Enlace actualizado", socialLinkService.updateLink(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        socialLinkService.deleteLink(id);
        return ResponseEntity.ok(ApiResponse.success("Enlace eliminado", null));
    }
}
