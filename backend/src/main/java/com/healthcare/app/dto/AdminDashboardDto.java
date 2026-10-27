package com.healthcare.app.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AdminDashboardDto {
    private int totalDoctors;
    private int totalPatients;
    private int todayAppointments;
    private int pendingAppointments;
    private int completedAppointments;
    private int cancelledAppointments;
    private BigDecimal totalRevenueCollected;

    private List<AppointmentDto> recentAppointments;
    private Map<String, Integer> appointmentsByStatus;
    private Map<String, Integer> appointmentsBySpecialization;
    private Map<String, Integer> appointmentsLast7Days;

    public AdminDashboardDto() {}

    public int getTotalDoctors() { return totalDoctors; }
    public void setTotalDoctors(int totalDoctors) { this.totalDoctors = totalDoctors; }

    public int getTotalPatients() { return totalPatients; }
    public void setTotalPatients(int totalPatients) { this.totalPatients = totalPatients; }

    public int getTodayAppointments() { return todayAppointments; }
    public void setTodayAppointments(int todayAppointments) { this.todayAppointments = todayAppointments; }

    public int getPendingAppointments() { return pendingAppointments; }
    public void setPendingAppointments(int pendingAppointments) { this.pendingAppointments = pendingAppointments; }

    public int getCompletedAppointments() { return completedAppointments; }
    public void setCompletedAppointments(int completedAppointments) { this.completedAppointments = completedAppointments; }

    public int getCancelledAppointments() { return cancelledAppointments; }
    public void setCancelledAppointments(int cancelledAppointments) { this.cancelledAppointments = cancelledAppointments; }

    public BigDecimal getTotalRevenueCollected() { return totalRevenueCollected; }
    public void setTotalRevenueCollected(BigDecimal totalRevenueCollected) { this.totalRevenueCollected = totalRevenueCollected; }

    public List<AppointmentDto> getRecentAppointments() { return recentAppointments; }
    public void setRecentAppointments(List<AppointmentDto> recentAppointments) { this.recentAppointments = recentAppointments; }

    public Map<String, Integer> getAppointmentsByStatus() { return appointmentsByStatus; }
    public void setAppointmentsByStatus(Map<String, Integer> appointmentsByStatus) { this.appointmentsByStatus = appointmentsByStatus; }

    public Map<String, Integer> getAppointmentsBySpecialization() { return appointmentsBySpecialization; }
    public void setAppointmentsBySpecialization(Map<String, Integer> appointmentsBySpecialization) { this.appointmentsBySpecialization = appointmentsBySpecialization; }

    public Map<String, Integer> getAppointmentsLast7Days() { return appointmentsLast7Days; }
    public void setAppointmentsLast7Days(Map<String, Integer> appointmentsLast7Days) { this.appointmentsLast7Days = appointmentsLast7Days; }
}
