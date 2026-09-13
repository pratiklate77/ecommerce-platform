package com.ecommerce.user_service.service;

import com.ecommerce.user_service.dto.AuthResponse;
import com.ecommerce.user_service.dto.LoginRequest;
import com.ecommerce.user_service.dto.RegisterRequest;
import com.ecommerce.user_service.dto.UserResponse;
import com.ecommerce.user_service.exception.ConflictException;
import com.ecommerce.user_service.exception.ResourceNotFoundException;
import com.ecommerce.user_service.exception.UnauthorizedException;
import com.ecommerce.user_service.model.Role;
import com.ecommerce.user_service.model.User;
import com.ecommerce.user_service.repository.UserRepository;
import com.ecommerce.user_service.security.CustomUserDetailsService;
import com.ecommerce.user_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with email " + email + " already exists");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        return issueToken(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        return UserResponse.from(findByEmailOrThrow(email.toLowerCase()));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    public User findByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private AuthResponse issueToken(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), UserResponse.from(user));
    }
}