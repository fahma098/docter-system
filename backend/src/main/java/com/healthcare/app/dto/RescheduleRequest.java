package com.healthcare.app.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public class RescheduleRequest {
    @NotNull(message = "New appointment date is required")
    private LocalDate newDate;

    @NotNull(message = "New start time is required")
    private LocalTime newStartTime;

    private String reason;

    public RescheduleRequest() {}

    public LocalDate getNewDate() { return newDate; }
    public void setNewDate(LocalDate newDate) { this.newDate = newDate; }

    public LocalTime getNewStartTime() { return newStartTime; }
    public void setNewStartTime(LocalTime newStartTime) { this.newStartTime = newStartTime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
