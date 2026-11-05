package com.joyeria.repository;

import com.joyeria.model.User;
import com.joyeria.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    List<User> findByRole(UserRole role);
    List<User> findByActive(Boolean active);
    long countByRole(UserRole role);
    long countByRoleAndActiveTrue(UserRole role);
}
