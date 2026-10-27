package com.healthcare.app.dto;

import java.time.LocalDate;
import java.util.List;

public class DayAvailabilityDto {
    private Long doctorId;
    private String doctorName;
    private LocalDate date;
    private String dayOfWeek;
    private boolean workingDay;
    private boolean onLeave;
    private List<AvailableSlotDto> slots;
    private int totalSlots;
    private int availableSlotsCount;

    public DayAvailabilityDto() {}

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public boolean isWorkingDay() { return workingDay; }
    public void setWorkingDay(boolean workingDay) { this.workingDay = workingDay; }

    public boolean isOnLeave() { return onLeave; }
    public void setOnLeave(boolean onLeave) { this.onLeave = onLeave; }

    public List<AvailableSlotDto> getSlots() { return slots; }
    public void setSlots(List<AvailableSlotDto> slots) { this.slots = slots; }

    public int getTotalSlots() { return totalSlots; }
    public void setTotalSlots(int totalSlots) { this.totalSlots = totalSlots; }

    public int getAvailableSlotsCount() { return availableSlotsCount; }
    public void setAvailableSlotsCount(int availableSlotsCount) { this.availableSlotsCount = availableSlotsCount; }
}
