package com.joyeria.repository;

import com.joyeria.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    Optional<Material> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByName(String name);
    List<Material> findByActiveTrue();
}
