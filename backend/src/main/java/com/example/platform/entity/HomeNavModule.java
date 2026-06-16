package com.example.platform.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "home_nav_modules")
public class HomeNavModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private Code code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "nav_label", length = 100)
    private String navLabel;

    @Column(name = "nav_path", length = 200)
    private String navPath;

    @Column(nullable = false)
    private Boolean visible = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public enum Code {
        SKILLS,
        GITHUB_TRENDING,
        ARTICLES,
        FORUM,
        NEWS,
        LLM_LEADERBOARD
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Code getCode() { return code; }
    public void setCode(Code code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNavLabel() { return navLabel; }
    public void setNavLabel(String navLabel) { this.navLabel = navLabel; }
    public String getNavPath() { return navPath; }
    public void setNavPath(String navPath) { this.navPath = navPath; }
    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { this.visible = visible; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
