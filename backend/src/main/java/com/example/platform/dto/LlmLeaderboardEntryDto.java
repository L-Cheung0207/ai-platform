package com.example.platform.dto;

import com.example.platform.entity.LlmLeaderboardEntry;

import java.time.Instant;

public class LlmLeaderboardEntryDto {

    private Long id;
    private String modelName;
    private String modelUrl;
    private String rankBadge;
    private Integer arenaElo;
    private Integer coding;
    private Integer vision;
    private Integer aaii;
    private Integer mmluPro;
    private Integer arcAgi;
    private String organization;
    private String licenseName;
    private int displayOrder;
    private Instant scrapedAt;

    public static LlmLeaderboardEntryDto fromEntity(LlmLeaderboardEntry e) {
        LlmLeaderboardEntryDto dto = new LlmLeaderboardEntryDto();
        dto.setId(e.getId());
        dto.setModelName(e.getModelName());
        dto.setModelUrl(e.getModelUrl());
        dto.setRankBadge(e.getRankBadge());
        dto.setArenaElo(e.getArenaElo());
        dto.setCoding(e.getCoding());
        dto.setVision(e.getVision());
        dto.setAaii(e.getAaii());
        dto.setMmluPro(e.getMmluPro());
        dto.setArcAgi(e.getArcAgi());
        dto.setOrganization(e.getOrganization());
        dto.setLicenseName(e.getLicenseName());
        dto.setDisplayOrder(e.getDisplayOrder());
        dto.setScrapedAt(e.getScrapedAt());
        return dto;
    }

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
}
