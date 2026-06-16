package com.example.platform.config;

import com.example.platform.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        // 公开：首页、分类、标签、Skill/Rule/AI知识库/资讯 列表与详情
                        .requestMatchers(HttpMethod.GET, "/api/home", "/api/home/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags", "/api/tags/**").permitAll()
                        .requestMatchers("/api/skills/me", "/api/skills/me/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/skills", "/api/skills/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/rules", "/api/rules/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles", "/api/articles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/news", "/api/news/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ai-tools", "/api/ai-tools/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/mcp", "/api/mcp/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/llm-leaderboard", "/api/llm-leaderboard/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/forum/categories", "/api/forum/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/forum/tags", "/api/forum/tags/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/forum/posts", "/api/forum/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/skills/*/usage", "/api/skills/*/feedback").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/integrations/skill-telemetry",
                                "/api/integrations/skill-telemetry/gitlab").permitAll()
                        // 认证：登录、注册
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        // 需认证（管理后台登录后获取当前用户）
                        .requestMatchers("/api/auth/me").authenticated()
                        // 管理端：仅 ADMIN（含 Skill/Rule 创建、编辑、删除）
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tags", "/api/articles", "/api/news").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/articles/**", "/api/news/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/articles/**", "/api/news/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
