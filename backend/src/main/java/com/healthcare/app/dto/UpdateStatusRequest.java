package com.healthcare.app.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateStatusRequest {
    @NotBlank(message = "New status is required")
    private String newStatus; // CONFIRMED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW

    private String reason;
    private String doctorInstructions;

    public UpdateStatusRequest() {}

    public UpdateStatusRequest(String newStatus, String reason, String doctorInstructions) {
        this.newStatus = newStatus;
        this.reason = reason;
        this.doctorInstructions = doctorInstructions;
    }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDoctorInstructions() { return doctorInstructions; }
    public void setDoctorInstructions(String doctorInstructions) { this.doctorInstructions = doctorInstructions; }
}
