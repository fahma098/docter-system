package com.healthcare.app.dto;

import java.math.BigDecimal;

public class UpdateDoctorRequest {
    private String fullName;
    private String phone;
    private Long specializationId;
    private String qualifications;
    private Integer experienceYears;
    private BigDecimal consultationFee;
    private Integer defaultAppointmentDuration;
    private String professionalBio;
    private String profileImageUrl;
    private String approvalStatus;

    public UpdateDoctorRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getSpecializationId() { return specializationId; }
    public void setSpecializationId(Long specializationId) { this.specializationId = specializationId; }

    public String getQualifications() { return qualifications; }
    public void setQualifications(String qualifications) { this.qualifications = qualifications; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }

    public Integer getDefaultAppointmentDuration() { return defaultAppointmentDuration; }
    public void setDefaultAppointmentDuration(Integer defaultAppointmentDuration) { this.defaultAppointmentDuration = defaultAppointmentDuration; }

    public String getProfessionalBio() { return professionalBio; }
    public void setProfessionalBio(String professionalBio) { this.professionalBio = professionalBio; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
}
