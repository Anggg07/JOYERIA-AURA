package com.joyeria.dto;

import com.joyeria.model.SupportTicket.TicketCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketCreateRequest {
    @NotBlank
    private String subject;
    @NotBlank
    private String message;
    @NotNull
    private TicketCategory category;
}
