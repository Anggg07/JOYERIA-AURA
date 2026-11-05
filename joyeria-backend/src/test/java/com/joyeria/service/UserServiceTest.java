package com.joyeria.service;

import com.joyeria.model.User;
import com.joyeria.model.UserRole;
import com.joyeria.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

    private User user(Long id, UserRole role, boolean active) {
        User u = new User();
        u.setId(id);
        u.setFirstName("N");
        u.setLastName("A");
        u.setEmail("u" + id + "@example.com");
        u.setRole(role);
        u.setActive(active);
        return u;
    }

    @Test
    void updateUserRole_DemotingLastActiveAdmin_Throws() {
        User admin = user(1L, UserRole.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndActiveTrue(UserRole.ADMIN)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> userService.updateUserRole(1L, "USER"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRole_DemotingAdmin_WhenAnotherActiveAdminExists_Allows() {
        User admin = user(1L, UserRole.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndActiveTrue(UserRole.ADMIN)).thenReturn(2L);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        var dto = userService.updateUserRole(1L, "USER");

        assertEquals(UserRole.USER, dto.getRole());
    }

    @Test
    void updateUserRole_DemotingInactiveAdmin_Allows() {
        User admin = user(1L, UserRole.ADMIN, false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        var dto = userService.updateUserRole(1L, "USER");

        assertEquals(UserRole.USER, dto.getRole());
        verify(userRepository, never()).countByRoleAndActiveTrue(any());
    }

    @Test
    void updateUserRole_PromotingRegularUser_Allows() {
        User regular = user(2L, UserRole.USER, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(regular));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        var dto = userService.updateUserRole(2L, "ADMIN");

        assertEquals(UserRole.ADMIN, dto.getRole());
    }

    @Test
    void toggleUserActive_DeactivatingLastActiveAdmin_Throws() {
        User admin = user(1L, UserRole.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndActiveTrue(UserRole.ADMIN)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> userService.toggleUserActive(1L));
        verify(userRepository, never()).save(any());
    }

    @Test
    void toggleUserActive_DeactivatingRegularUser_Toggles() {
        User regular = user(3L, UserRole.USER, true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(regular));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        var dto = userService.toggleUserActive(3L);

        assertFalse(dto.getActive());
    }

    @Test
    void toggleUserActive_ReactivatingAdmin_Allows() {
        User admin = user(1L, UserRole.ADMIN, false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        var dto = userService.toggleUserActive(1L);

        assertTrue(dto.getActive());
        verify(userRepository, never()).countByRoleAndActiveTrue(any());
    }
}
