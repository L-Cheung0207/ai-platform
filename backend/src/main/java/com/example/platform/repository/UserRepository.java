package com.example.platform.repository;

import com.example.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findFirstByRole(User.Role role);

    boolean existsByUsername(String username);

    boolean existsByRole(User.Role role);

    long countByRole(User.Role role);
}
