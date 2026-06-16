package com.example.platform.dto;

public class HomeNavModuleUpdateRequest {

    private Boolean visible;
    private Integer sortOrder;

    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { this.visible = visible; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
