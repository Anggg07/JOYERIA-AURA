package com.joyeria.dto;

import com.joyeria.model.SupportTicket.TicketCategory;
import com.joyeria.model.SupportTicket.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketDTO {
    private Long id;
    private String ticketNumber;
    private String subject;
    private String message;
    private TicketCategory category;
    private TicketStatus status;
    private String customerName;
    private String customerEmail;
    private List<TicketResponseDTO> responses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
