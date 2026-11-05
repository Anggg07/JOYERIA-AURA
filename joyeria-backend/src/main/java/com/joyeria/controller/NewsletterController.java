package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.service.NewsletterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<String>> subscribe(@Valid @RequestBody NewsletterRequest request) {
        String message = newsletterService.subscribe(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<ApiResponse<String>> unsubscribe(@RequestParam String email) {
        String message = newsletterService.unsubscribe(email);
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}
