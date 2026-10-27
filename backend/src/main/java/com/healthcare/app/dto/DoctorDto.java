package com.healthcare.app.dto;

import java.math.BigDecimal;
import java.util.List;

public class DoctorDto {
    private Long doctorId;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private Long specializationId;
    private String specializationName;
    private String licenseNumber;
    private String qualifications;
    private int experienceYears;
    private BigDecimal consultationFee;
    private int defaultAppointmentDuration;
    private String professionalBio;
    private String profileImageUrl;
    private String approvalStatus;
    private List<ScheduleDto> schedules;
    private String nextAvailableSlot;

    public DoctorDto() {}

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getSpecializationId() { return specializationId; }
    public void setSpecializationId(Long specializationId) { this.specializationId = specializationId; }

    public String getSpecializationName() { return specializationName; }
    public void setSpecializationName(String specializationName) { this.specializationName = specializationName; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getQualifications() { return qualifications; }
    public void setQualifications(String qualifications) { this.qualifications = qualifications; }

    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }

    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }

    public int getDefaultAppointmentDuration() { return defaultAppointmentDuration; }
    public void setDefaultAppointmentDuration(int defaultAppointmentDuration) { this.defaultAppointmentDuration = defaultAppointmentDuration; }

    public String getProfessionalBio() { return professionalBio; }
    public void setProfessionalBio(String professionalBio) { this.professionalBio = professionalBio; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public List<ScheduleDto> getSchedules() { return schedules; }
    public void setSchedules(List<ScheduleDto> schedules) { this.schedules = schedules; }

    public String getNextAvailableSlot() { return nextAvailableSlot; }
    public void setNextAvailableSlot(String nextAvailableSlot) { this.nextAvailableSlot = nextAvailableSlot; }
}
