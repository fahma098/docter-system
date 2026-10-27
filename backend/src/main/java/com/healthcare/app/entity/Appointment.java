package com.healthcare.app.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Appointment {
    private Long appointmentId;
    private String appointmentReference;
    private Long patientId;
    private Long doctorId;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String appointmentStatus; // PENDING_CONFIRMATION, CONFIRMED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
    private BigDecimal consultationFeeSnapshot;
    private String appointmentType; // IN_PERSON, VIDEO_CONSULTATION, FOLLOW_UP
    private String patientNotes;
    private String cancellationReason;
    private String doctorInstructions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Joined patient fields
    private String patientName;
    private String patientEmail;
    private String patientPhone;

    // Joined doctor fields
    private String doctorName;
    private String doctorEmail;
    private String doctorPhone;
    private String specializationName;
    private String doctorQualifications;

    // Payment fields
    private String paymentStatus;
    private BigDecimal paymentAmount;
    private String paymentMethod;
    private String transactionReference;

    public Appointment() {}

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public String getAppointmentReference() { return appointmentReference; }
    public void setAppointmentReference(String appointmentReference) { this.appointmentReference = appointmentReference; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getAppointmentStatus() { return appointmentStatus; }
    public void setAppointmentStatus(String appointmentStatus) { this.appointmentStatus = appointmentStatus; }

    public BigDecimal getConsultationFeeSnapshot() { return consultationFeeSnapshot; }
    public void setConsultationFeeSnapshot(BigDecimal consultationFeeSnapshot) { this.consultationFeeSnapshot = consultationFeeSnapshot; }

    public String getAppointmentType() { return appointmentType; }
    public void setAppointmentType(String appointmentType) { this.appointmentType = appointmentType; }

    public String getPatientNotes() { return patientNotes; }
    public void setPatientNotes(String patientNotes) { this.patientNotes = patientNotes; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public String getDoctorInstructions() { return doctorInstructions; }
    public void setDoctorInstructions(String doctorInstructions) { this.doctorInstructions = doctorInstructions; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorEmail() { return doctorEmail; }
    public void setDoctorEmail(String doctorEmail) { this.doctorEmail = doctorEmail; }

    public String getDoctorPhone() { return doctorPhone; }
    public void setDoctorPhone(String doctorPhone) { this.doctorPhone = doctorPhone; }

    public String getSpecializationName() { return specializationName; }
    public void setSpecializationName(String specializationName) { this.specializationName = specializationName; }

    public String getDoctorQualifications() { return doctorQualifications; }
    public void setDoctorQualifications(String doctorQualifications) { this.doctorQualifications = doctorQualifications; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
}
