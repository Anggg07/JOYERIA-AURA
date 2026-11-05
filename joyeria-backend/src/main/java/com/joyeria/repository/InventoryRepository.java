package com.joyeria.repository;

import com.joyeria.model.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findByProductIdOrderByCreatedAtDesc(Long productId);
    Page<Inventory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
