package com.joyeria.repository;

import com.joyeria.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByProviderReference(String reference);
    Optional<Payment> findByPreferenceId(String preferenceId);
    Optional<Payment> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId, Payment.PaymentStatus status);
}
