package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.dto.AdminUserCreateRequest;
import com.example.platform.dto.AdminUserDto;
import com.example.platform.dto.AdminUserUpdateRequest;
import com.example.platform.dto.UserRoleRequest;
import com.example.platform.dto.UserSkillGovernanceRoleRequest;
import com.example.platform.entity.SkillReview.ReviewerRole;
import com.example.platform.entity.User;
import com.example.platform.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminService {

    private static final int PAGE_SIZE_LIMIT = 50;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDto> listUsers(int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(0, page - 1),
                Math.min(PAGE_SIZE_LIMIT, Math.max(1, size)),
                Sort.by(Sort.Direction.ASC, "id")
        );
        return userRepository.findAll(pageRequest).map(AdminUserDto::fromEntity);
    }

    @Transactional
    public AdminUserDto createUser(AdminUserCreateRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role() != null ? request.role() : User.Role.NORMAL);
        user.setSkillGovernanceRole(request.skillGovernanceRole() != null
                ? request.skillGovernanceRole()
                : ReviewerRole.CONTRIBUTOR);
        return AdminUserDto.fromEntity(userRepository.save(user));
    }

    @Transactional
    public AdminUserDto updateUser(Long userId, AdminUserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        assertCanChangeRole(user, request.role());
        user.setRole(request.role());
        user.setSkillGovernanceRole(request.skillGovernanceRole());
        return AdminUserDto.fromEntity(userRepository.save(user));
    }

    @Transactional
    public AdminUserDto updateRole(Long userId, UserRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        assertCanChangeRole(user, request.role());
        user.setRole(request.role());
        return AdminUserDto.fromEntity(userRepository.save(user));
    }

    @Transactional
    public AdminUserDto updateSkillGovernanceRole(Long userId, UserSkillGovernanceRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        user.setSkillGovernanceRole(request.skillGovernanceRole());
        return AdminUserDto.fromEntity(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId, Long currentAdminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        if (user.getId().equals(currentAdminId)) {
            throw new IllegalArgumentException("不能删除当前登录用户");
        }
        if (user.getRole() == User.Role.ADMIN && userRepository.countByRole(User.Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("至少需要保留一个管理员");
        }
        userRepository.delete(user);
    }

    private void assertCanChangeRole(User user, User.Role nextRole) {
        if (user.getRole() == User.Role.ADMIN
                && nextRole != User.Role.ADMIN
                && userRepository.countByRole(User.Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("至少需要保留一个管理员");
        }
    }
}
