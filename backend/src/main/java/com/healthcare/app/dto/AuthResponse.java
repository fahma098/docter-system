package com.healthcare.app.dto;

public class AuthResponse {
    private String token;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String role; // PATIENT, DOCTOR, ADMIN
    private Long patientId;
    private Long doctorId;

    public AuthResponse() {}

    public AuthResponse(String token, Long userId, String fullName, String email, String phone, String role, Long patientId, Long doctorId) {
        this.token = token;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.patientId = patientId;
        this.doctorId = doctorId;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
}
