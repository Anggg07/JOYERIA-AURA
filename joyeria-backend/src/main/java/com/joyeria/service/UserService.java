package com.joyeria.service;

import com.joyeria.dto.*;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.User;
import com.joyeria.model.UserRole;
import com.joyeria.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return mapToDTO(user);
    }

    public UserDTO updateProfile(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        return mapToDTO(userRepository.save(user));
    }

    public void changePassword(String email, PasswordChangeRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());
    }

    public PagedResponse<UserDTO> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userRepository.findAll(pageable);
        return PagedResponse.<UserDTO>builder()
                .content(users.getContent().stream().map(this::mapToDTO).toList())
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .first(users.isFirst())
                .last(users.isLast())
                .build();
    }

    public UserDTO updateUserRole(Long id, String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        UserRole newRole = UserRole.valueOf(role);
        if (user.getRole() == UserRole.ADMIN && newRole != UserRole.ADMIN
                && user.getActive() && userRepository.countByRoleAndActiveTrue(UserRole.ADMIN) <= 1) {
            throw new IllegalStateException("No se puede quitar el rol de administrador al último administrador activo");
        }
        user.setRole(newRole);
        return mapToDTO(userRepository.save(user));
    }

    public UserDTO toggleUserActive(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (user.getActive() && user.getRole() == UserRole.ADMIN
                && userRepository.countByRoleAndActiveTrue(UserRole.ADMIN) <= 1) {
            throw new IllegalStateException("No se puede desactivar al último administrador activo");
        }
        user.setActive(!user.getActive());
        return mapToDTO(userRepository.save(user));
    }

    public Long countUsers() {
        return userRepository.count();
    }

    public Long countActiveUsers() {
        return userRepository.findByActive(true).stream().filter(u -> u.getRole() == UserRole.USER).count();
    }

    public UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
