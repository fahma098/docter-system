package com.healthcare.app.dto;

import java.time.LocalTime;

public class AvailableSlotDto {
    private LocalTime startTime;
    private LocalTime endTime;
    private String formattedSlot;
    private boolean available;
    private String reason; // e.g., "BOOKED", "BREAK", "LEAVE", "PAST"

    public AvailableSlotDto() {}

    public AvailableSlotDto(LocalTime startTime, LocalTime endTime, String formattedSlot, boolean available, String reason) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.formattedSlot = formattedSlot;
        this.available = available;
        this.reason = reason;
    }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getFormattedSlot() { return formattedSlot; }
    public void setFormattedSlot(String formattedSlot) { this.formattedSlot = formattedSlot; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
