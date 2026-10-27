package com.healthcare.app.entity;

import java.time.LocalDateTime;

public class DoctorUnavailablePeriod {
    private Long unavailableId;
    private Long doctorId;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String reason;
    private String approvalStatus; // PENDING, APPROVED, REJECTED
    private LocalDateTime createdAt;

    public DoctorUnavailablePeriod() {}

    public Long getUnavailableId() { return unavailableId; }
    public void setUnavailableId(Long unavailableId) { this.unavailableId = unavailableId; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public LocalDateTime getStartDatetime() { return startDatetime; }
    public void setStartDatetime(LocalDateTime startDatetime) { this.startDatetime = startDatetime; }

    public LocalDateTime getEndDatetime() { return endDatetime; }
    public void setEndDatetime(LocalDateTime endDatetime) { this.endDatetime = endDatetime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
