package com.healthcare.app.dto;

import java.math.BigDecimal;
import java.util.List;

public class DoctorDashboardDto {
    private String doctorName;
    private String specialization;
    private int todayCount;
    private int upcomingCount;
    private int completedTodayCount;
    private int pendingCount;
    private int totalPatients;
    private BigDecimal monthlyEarnings;
    private List<AppointmentDto> todayAppointments;
    private List<AppointmentDto> upcomingAppointments;
    private List<ScheduleDto> currentSchedules;

    public DoctorDashboardDto() {}

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public int getTodayCount() { return todayCount; }
    public void setTodayCount(int todayCount) { this.todayCount = todayCount; }

    public int getUpcomingCount() { return upcomingCount; }
    public void setUpcomingCount(int upcomingCount) { this.upcomingCount = upcomingCount; }

    public int getCompletedTodayCount() { return completedTodayCount; }
    public void setCompletedTodayCount(int completedTodayCount) { this.completedTodayCount = completedTodayCount; }

    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }

    public int getTotalPatients() { return totalPatients; }
    public void setTotalPatients(int totalPatients) { this.totalPatients = totalPatients; }

    public BigDecimal getMonthlyEarnings() { return monthlyEarnings; }
    public void setMonthlyEarnings(BigDecimal monthlyEarnings) { this.monthlyEarnings = monthlyEarnings; }

    public List<AppointmentDto> getTodayAppointments() { return todayAppointments; }
    public void setTodayAppointments(List<AppointmentDto> todayAppointments) { this.todayAppointments = todayAppointments; }

    public List<AppointmentDto> getUpcomingAppointments() { return upcomingAppointments; }
    public void setUpcomingAppointments(List<AppointmentDto> upcomingAppointments) { this.upcomingAppointments = upcomingAppointments; }

    public List<ScheduleDto> getCurrentSchedules() { return currentSchedules; }
    public void setCurrentSchedules(List<ScheduleDto> currentSchedules) { this.currentSchedules = currentSchedules; }
}
