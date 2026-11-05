package com.joyeria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponseDTO {
    private Long id;
    private String message;
    private String authorName;
    private String authorRole;
    private boolean staff;
    private LocalDateTime createdAt;
}
