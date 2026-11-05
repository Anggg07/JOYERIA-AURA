package com.joyeria.service;

import com.joyeria.dto.AuthRequest;
import com.joyeria.dto.AuthResponse;
import com.joyeria.dto.RegisterRequest;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.User;
import com.joyeria.model.UserRole;
import com.joyeria.repository.UserRepository;
import com.joyeria.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!user.getActive()) {
            throw new IllegalStateException("Usuario desactivado");
        }

        return buildAuthResponse(user, tokenProvider.generateToken(authentication));
    }

    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        if (userRepository.existsByEmail(request.getEmail()) || userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException(
                    "No se pudo crear la cuenta con esos datos. Si ya tienes una cuenta, inicia sesión.");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        user.setActive(true);

        user = userRepository.save(user);

        return buildAuthResponse(user, tokenProvider.generateToken(user.getEmail()));
    }

    public AuthResponse refresh(String refreshToken) {
        User user = refreshTokenService.verifyAndRotate(refreshToken);
        return buildAuthResponse(user, tokenProvider.generateToken(user.getEmail()));
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken) {
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshTokenService.issueToken(user))
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}
