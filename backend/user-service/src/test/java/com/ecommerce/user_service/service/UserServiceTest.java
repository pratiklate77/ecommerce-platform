package com.ecommerce.user_service.service;

import com.ecommerce.user_service.dto.AuthResponse;
import com.ecommerce.user_service.dto.LoginRequest;
import com.ecommerce.user_service.dto.RegisterRequest;
import com.ecommerce.user_service.exception.ConflictException;
import com.ecommerce.user_service.exception.UnauthorizedException;
import com.ecommerce.user_service.model.Role;
import com.ecommerce.user_service.model.User;
import com.ecommerce.user_service.repository.UserRepository;
import com.ecommerce.user_service.security.CustomUserDetailsService;
import com.ecommerce.user_service.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    private UserDetails userDetailsFor(String email) {
        return org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password("encoded")
                .authorities("ROLE_CUSTOMER")
                .build();
    }

    @Test
    void registerCreatesUserEncodesPasswordAndReturnsToken() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserDetails userDetails = userDetailsFor("alice@example.com");
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = userService.register(
                new RegisterRequest("Alice@Example.com", "password123", "Alice", "Smith", "555-1234"));

        assertEquals("alice@example.com", response.user().email());
        assertEquals("jwt-token", response.token());
        assertEquals(Role.CUSTOMER, response.user().role());
        verify(userRepository).save(argThat(user -> user.getPassword().equals("encoded-password")));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.register(
                new RegisterRequest("alice@example.com", "password123", null, null, null)));
    }

    @Test
    void loginReturnsTokenOnValidCredentials() {
        User user = User.builder()
                .id(1L)
                .email("alice@example.com")
                .password("encoded-password")
                .firstName("Alice")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

        UserDetails userDetails = userDetailsFor("alice@example.com");
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = userService.login(new LoginRequest("Alice@Example.com", "password123"));

        assertEquals("jwt-token", response.token());
        assertEquals("alice@example.com", response.user().email());
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = User.builder()
                .email("alice@example.com")
                .password("encoded-password")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> userService.login(
                new LoginRequest("alice@example.com", "wrong-password")));
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> userService.login(
                new LoginRequest("nobody@example.com", "whatever")));
    }
}