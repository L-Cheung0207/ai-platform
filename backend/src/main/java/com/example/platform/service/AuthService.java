package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.dto.AuthResponse;
import com.example.platform.entity.User;
import com.example.platform.repository.UserRepository;
import com.example.platform.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("用户名或密码错误"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new org.springframework.security.authentication.BadCredentialsException("用户名或密码错误");
        }
        return buildResponse(user);
    }

    @Transactional
    public AuthResponse register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(User.Role.NORMAL);
        user = userRepository.save(user);
        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse.UserInfo getMe(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        AuthResponse.UserInfo info = new AuthResponse.UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setRole(user.getRole().name());
        return info;
    }

    private AuthResponse buildResponse(User user) {
        AuthResponse res = new AuthResponse();
        res.setToken(jwtService.generateToken(user));
        AuthResponse.UserInfo info = new AuthResponse.UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setRole(user.getRole().name());
        res.setUser(info);
        return res;
    }
}
