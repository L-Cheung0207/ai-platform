package com.example.platform.config;

import com.example.platform.entity.Category;
import com.example.platform.entity.User;
import com.example.platform.repository.CategoryRepository;
import com.example.platform.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public DataInitializer(UserRepository userRepository, CategoryRepository categoryRepository,
                           PasswordEncoder passwordEncoder, Environment environment) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategoriesIfEmpty();
        seedAdminIfNone();
    }

    private void seedCategoriesIfEmpty() {
        if (categoryRepository.count() > 0) return;
        String[][] defaults = {
            { "Skill/Rule", "SKILL_RULE", "10" },
            { "学习文章", "ARTICLE", "20" },
        };
        for (int i = 0; i < defaults.length; i++) {
            Category c = new Category();
            c.setName(defaults[i][0]);
            c.setType(defaults[i][1]);
            c.setSortOrder(Integer.parseInt(defaults[i][2]));
            categoryRepository.save(c);
        }
    }

    private void seedAdminIfNone() {
        if (userRepository.existsByRole(User.Role.ADMIN)) {
            return;
        }
        String username = environment.getProperty("app.admin.init-username", "admin");
        String password = environment.getProperty("app.admin.init-password", "admin123");
        if (userRepository.existsByUsername(username)) {
            User existing = userRepository.findByUsername(username).orElseThrow();
            existing.setRole(User.Role.ADMIN);
            userRepository.save(existing);
            return;
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        userRepository.save(admin);
    }
}
