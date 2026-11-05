package com.joyeria.repository;

import com.joyeria.model.SupportTicket;
import com.joyeria.model.SupportTicket.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    Optional<SupportTicket> findByTicketNumber(String ticketNumber);
    Page<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<SupportTicket> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);
    long countByStatusIn(java.util.Collection<TicketStatus> statuses);
}
