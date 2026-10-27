package com.healthcare.app.controller;

import com.healthcare.app.dto.ApiResponse;
import com.healthcare.app.dto.PatientDashboardDto;
import com.healthcare.app.entity.Patient;
import com.healthcare.app.exception.ResourceNotFoundException;
import com.healthcare.app.repository.PatientRepository;
import com.healthcare.app.security.RequiresRole;
import com.healthcare.app.security.SecurityContextHelper;
import com.healthcare.app.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ReportService reportService;

    @GetMapping("/dashboard")
    @RequiresRole("PATIENT")
    public ResponseEntity<ApiResponse<PatientDashboardDto>> getDashboard() {
        Long patientId = SecurityContextHelper.getCurrentPatientId();
        PatientDashboardDto dto = reportService.getPatientDashboardData(patientId);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/profile")
    @RequiresRole("PATIENT")
    public ResponseEntity<ApiResponse<Patient>> getProfile() {
        Long patientId = SecurityContextHelper.getCurrentPatientId();
        Patient p = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        return ResponseEntity.ok(ApiResponse.ok(p));
    }

    @PutMapping("/profile")
    @RequiresRole("PATIENT")
    public ResponseEntity<ApiResponse<Patient>> updateProfile(@RequestBody Patient update) {
        Long patientId = SecurityContextHelper.getCurrentPatientId();
        Patient existing = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        if (update.getDateOfBirth() != null) existing.setDateOfBirth(update.getDateOfBirth());
        if (update.getGender() != null) existing.setGender(update.getGender());
        if (update.getBloodGroup() != null) existing.setBloodGroup(update.getBloodGroup());
        if (update.getAddress() != null) existing.setAddress(update.getAddress());
        if (update.getEmergencyContact() != null) existing.setEmergencyContact(update.getEmergencyContact());

        patientRepository.update(existing);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", existing));
    }
}
