package com.joyeria.service;

import com.joyeria.dto.SupportTicketDTO;
import com.joyeria.dto.TicketResponseRequest;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.SupportTicket;
import com.joyeria.model.User;
import com.joyeria.model.UserRole;
import com.joyeria.repository.SupportTicketRepository;
import com.joyeria.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportServiceTest {

    @Mock
    private SupportTicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SupportService supportService;

    private User user(Long id, UserRole role, String first, String last) {
        User u = new User();
        u.setId(id);
        u.setFirstName(first);
        u.setLastName(last);
        u.setEmail(first.toLowerCase() + "@example.com");
        u.setRole(role);
        u.setActive(true);
        return u;
    }

    private SupportTicket ticket(SupportTicket.TicketStatus status, User customer) {
        SupportTicket t = new SupportTicket();
        t.setId(10L);
        t.setTicketNumber("TKT-ABCD1234");
        t.setSubject("Ayuda");
        t.setMessage("Necesito ayuda");
        t.setCategory(SupportTicket.TicketCategory.ORDERS);
        t.setStatus(status);
        t.setUser(customer);
        return t;
    }

    @Test
    void addResponseAsAdmin_WhenOpen_AddsResponseAndSetsInProgress() {
        User customer = user(2L, UserRole.USER, "Maria", "Lopez");
        User admin = user(1L, UserRole.ADMIN, "Admin", "Root");
        SupportTicket t = ticket(SupportTicket.TicketStatus.OPEN, customer);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(t));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));

        SupportTicketDTO dto = supportService.addResponseAsAdmin(10L, "admin@example.com", new TicketResponseRequest("Hola"));

        assertEquals(SupportTicket.TicketStatus.IN_PROGRESS, dto.getStatus());
        assertEquals(1, dto.getResponses().size());
        assertEquals("Hola", dto.getResponses().get(0).getMessage());
        assertTrue(dto.getResponses().get(0).isStaff());
        assertEquals("ADMIN", dto.getResponses().get(0).getAuthorRole());
    }

    @Test
    void addResponseAsAdmin_WhenInProgress_KeepsStatus() {
        User customer = user(2L, UserRole.USER, "Maria", "Lopez");
        User admin = user(1L, UserRole.ADMIN, "Admin", "Root");
        SupportTicket t = ticket(SupportTicket.TicketStatus.IN_PROGRESS, customer);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(t));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));

        SupportTicketDTO dto = supportService.addResponseAsAdmin(10L, "admin@example.com", new TicketResponseRequest("Sigo aquí"));

        assertEquals(SupportTicket.TicketStatus.IN_PROGRESS, dto.getStatus());
        assertEquals(1, dto.getResponses().size());
    }

    @Test
    void addResponseAsAdmin_TicketNotFound_Throws() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> supportService.addResponseAsAdmin(99L, "admin@example.com", new TicketResponseRequest("x")));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void getTicketById_NotFound_Throws() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> supportService.getTicketById(99L));
    }

    @Test
    void getTicketById_MapsCustomerAndEmptyResponses() {
        User customer = user(2L, UserRole.USER, "Maria", "Lopez");
        SupportTicket t = ticket(SupportTicket.TicketStatus.OPEN, customer);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(t));

        SupportTicketDTO dto = supportService.getTicketById(10L);

        assertEquals("Maria Lopez", dto.getCustomerName());
        assertEquals("maria@example.com", dto.getCustomerEmail());
        assertTrue(dto.getResponses().isEmpty());
    }
}
