package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.AuthResponse;
import com.example.platform.dto.LoginRequest;
import com.example.platform.dto.RegisterRequest;
import com.example.platform.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse data = authService.login(req.getUsername(), req.getPassword());
        return ApiResponse.ok(data);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse data = authService.register(req.getUsername(), req.getPassword());
        return ApiResponse.ok(data);
    }

    @GetMapping("/me")
    public ApiResponse<AuthResponse.UserInfo> me(org.springframework.security.core.Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(authService.getMe(userId));
    }
}
