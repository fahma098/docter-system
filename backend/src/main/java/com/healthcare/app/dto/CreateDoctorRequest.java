package com.healthcare.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class CreateDoctorRequest {
    @NotBlank(message = "Doctor full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Temporary initial password is required")
    private String password;

    @NotNull(message = "Specialization ID is required")
    private Long specializationId;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotBlank(message = "Qualifications are required")
    private String qualifications;

    private int experienceYears;

    @NotNull(message = "Consultation fee is required")
    @Positive(message = "Consultation fee must be positive")
    private BigDecimal consultationFee;

    private int defaultAppointmentDuration = 30;

    private String professionalBio;
    private String profileImageUrl;

    public CreateDoctorRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Long getSpecializationId() { return specializationId; }
    public void setSpecializationId(Long specializationId) { this.specializationId = specializationId; }

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
}
