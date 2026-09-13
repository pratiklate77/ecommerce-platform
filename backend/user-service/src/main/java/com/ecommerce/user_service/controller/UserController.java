package com.ecommerce.user_service.controller;

import com.ecommerce.user_service.dto.UserResponse;
import com.ecommerce.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserDetails principal) {
        return userService.getCurrentUser(principal.getUsername());
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.listAll();
    }
}