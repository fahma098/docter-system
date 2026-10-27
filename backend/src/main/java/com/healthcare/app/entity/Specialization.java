package com.healthcare.app.entity;

import java.time.LocalDateTime;

public class Specialization {
    private Long specializationId;
    private String specializationName;
    private String description;
    private String iconName;
    private boolean activeStatus;
    private LocalDateTime createdAt;

    public Specialization() {}

    public Specialization(Long specializationId, String specializationName, String description, String iconName, boolean activeStatus) {
        this.specializationId = specializationId;
        this.specializationName = specializationName;
        this.description = description;
        this.iconName = iconName;
        this.activeStatus = activeStatus;
    }

    public Long getSpecializationId() { return specializationId; }
    public void setSpecializationId(Long specializationId) { this.specializationId = specializationId; }

    public String getSpecializationName() { return specializationName; }
    public void setSpecializationName(String specializationName) { this.specializationName = specializationName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    public boolean isActiveStatus() { return activeStatus; }
    public void setActiveStatus(boolean activeStatus) { this.activeStatus = activeStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
