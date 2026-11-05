package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/support")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupportController {

    private final SupportService supportService;

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<PagedResponse<SupportTicketDTO>>> getAllTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(supportService.getAllTickets(page, size)));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> getTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(supportService.getTicketById(id)));
    }

    @PostMapping("/tickets/{id}/responses")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> reply(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody TicketResponseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Respuesta enviada",
                supportService.addResponseAsAdmin(id, authentication.getName(), request)));
    }

    @PutMapping("/tickets/{id}/status")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> updateStatus(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Estado actualizado", supportService.updateTicketStatus(id, status)));
    }
}
