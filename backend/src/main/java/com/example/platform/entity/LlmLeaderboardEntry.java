package com.example.platform.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "llm_leaderboard")
public class LlmLeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name", nullable = false, length = 300)
    private String modelName;

    @Column(name = "model_url", length = 500)
    private String modelUrl;

    @Column(name = "rank_badge", length = 20)
    private String rankBadge;

    @Column(name = "arena_elo")
    private Integer arenaElo;

    @Column(name = "coding")
    private Integer coding;

    @Column(name = "vision")
    private Integer vision;

    @Column(name = "aaii")
    private Integer aaii;

    @Column(name = "mmlu_pro")
    private Integer mmluPro;

    @Column(name = "arc_agi")
    private Integer arcAgi;

    @Column(name = "organization", length = 200)
    private String organization;

    @Column(name = "license_name", length = 100)
    private String licenseName;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "scraped_at")
    private Instant scrapedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PreUpdate
    public void preUpdate() { /* display only entity, no updatedAt */ }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getModelUrl() { return modelUrl; }
    public void setModelUrl(String modelUrl) { this.modelUrl = modelUrl; }
    public String getRankBadge() { return rankBadge; }
    public void setRankBadge(String rankBadge) { this.rankBadge = rankBadge; }
    public Integer getArenaElo() { return arenaElo; }
    public void setArenaElo(Integer arenaElo) { this.arenaElo = arenaElo; }
    public Integer getCoding() { return coding; }
    public void setCoding(Integer coding) { this.coding = coding; }
    public Integer getVision() { return vision; }
    public void setVision(Integer vision) { this.vision = vision; }
    public Integer getAaii() { return aaii; }
    public void setAaii(Integer aaii) { this.aaii = aaii; }
    public Integer getMmluPro() { return mmluPro; }
    public void setMmluPro(Integer mmluPro) { this.mmluPro = mmluPro; }
    public Integer getArcAgi() { return arcAgi; }
    public void setArcAgi(Integer arcAgi) { this.arcAgi = arcAgi; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getLicenseName() { return licenseName; }
    public void setLicenseName(String licenseName) { this.licenseName = licenseName; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public Instant getScrapedAt() { return scrapedAt; }
    public void setScrapedAt(Instant scrapedAt) { this.scrapedAt = scrapedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
