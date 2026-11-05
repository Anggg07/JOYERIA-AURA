package com.joyeria.controller;

import com.joyeria.dto.ApiResponse;
import com.joyeria.dto.UploadResponse;
import com.joyeria.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/uploads")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<ApiResponse<UploadResponse>> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Imagen subida",
                new UploadResponse(fileStorageService.store(file))));
    }
}
