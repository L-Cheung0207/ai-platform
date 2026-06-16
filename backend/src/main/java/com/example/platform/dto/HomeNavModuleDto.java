package com.example.platform.dto;

import com.example.platform.entity.HomeNavModule;

public class HomeNavModuleDto {

    private Long id;
    private String code;
    private String name;
    private String navLabel;
    private String navPath;
    private Boolean visible;
    private Integer sortOrder;

    public static HomeNavModuleDto fromEntity(HomeNavModule entity) {
        HomeNavModuleDto dto = new HomeNavModuleDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode() != null ? entity.getCode().name() : null);
        dto.setName(entity.getName());
        dto.setNavLabel(entity.getNavLabel());
        dto.setNavPath(entity.getNavPath());
        dto.setVisible(entity.getVisible());
        dto.setSortOrder(entity.getSortOrder());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
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
}
