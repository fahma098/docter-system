package com.healthcare.app.dto;

import java.util.List;

public class PatientDashboardDto {
    private String patientName;
    private int upcomingCount;
    private int completedCount;
    private int pendingCount;
    private int cancelledCount;
    private List<AppointmentDto> upcomingAppointments;
    private List<AppointmentDto> pastAppointments;

    public PatientDashboardDto() {}

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public int getUpcomingCount() { return upcomingCount; }
    public void setUpcomingCount(int upcomingCount) { this.upcomingCount = upcomingCount; }

    public int getCompletedCount() { return completedCount; }
    public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }

    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }

    public int getCancelledCount() { return cancelledCount; }
    public void setCancelledCount(int cancelledCount) { this.cancelledCount = cancelledCount; }

    public List<AppointmentDto> getUpcomingAppointments() { return upcomingAppointments; }
    public void setUpcomingAppointments(List<AppointmentDto> upcomingAppointments) { this.upcomingAppointments = upcomingAppointments; }

    public List<AppointmentDto> getPastAppointments() { return pastAppointments; }
    public void setPastAppointments(List<AppointmentDto> pastAppointments) { this.pastAppointments = pastAppointments; }
}
