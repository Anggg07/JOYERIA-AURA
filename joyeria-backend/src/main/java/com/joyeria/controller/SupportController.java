package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<PagedResponse<SupportTicketDTO>>> getUserTickets(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(supportService.getUserTickets(authentication.getName(), page, size)));
    }

    @GetMapping("/tickets/{ticketNumber}")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> getTicket(
            Authentication authentication, @PathVariable String ticketNumber) {
        return ResponseEntity.ok(ApiResponse.success(
                supportService.getTicketByIdentifier(ticketNumber, authentication.getName())));
    }

    @PostMapping("/tickets")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> createTicket(
            Authentication authentication, @Valid @RequestBody SupportTicketCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Ticket creado", supportService.createTicket(authentication.getName(), request)));
    }

    @PostMapping("/tickets/{ticketNumber}/responses")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> addResponse(
            Authentication authentication,
            @PathVariable String ticketNumber,
            @Valid @RequestBody TicketResponseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Respuesta enviada",
                supportService.addResponse(ticketNumber, authentication.getName(), request)));
    }
}
