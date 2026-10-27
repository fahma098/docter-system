package com.healthcare.app.controller;

import com.healthcare.app.dto.*;
import com.healthcare.app.security.RequiresRole;
import com.healthcare.app.security.SecurityContextHelper;
import com.healthcare.app.service.AvailabilityService;
import com.healthcare.app.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private AvailabilityService availabilityService;

    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<List<DoctorDto>>> searchDoctors(
            @RequestParam(required = false) Long specializationId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "true") Boolean approvedOnly) {
        List<DoctorDto> list = doctorService.searchDoctors(specializationId, search, approvedOnly);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/doctors/{id}")
    public ResponseEntity<ApiResponse<DoctorDto>> getDoctorById(@PathVariable Long id) {
        DoctorDto dto = doctorService.getDoctorById(id);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/doctors/{id}/availability")
    public ResponseEntity<ApiResponse<DayAvailabilityDto>> getDoctorAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DayAvailabilityDto availability = availabilityService.getDoctorAvailabilityForDate(id, date);
        return ResponseEntity.ok(ApiResponse.ok(availability));
    }

    @PostMapping("/admin/doctors")
    @RequiresRole("ADMIN")
    public ResponseEntity<ApiResponse<DoctorDto>> createDoctor(@Valid @RequestBody CreateDoctorRequest req) {
        Long actorId = SecurityContextHelper.getCurrentUserId();
        DoctorDto created = doctorService.createDoctor(req, actorId);
        return ResponseEntity.ok(ApiResponse.ok("Doctor account created successfully", created));
    }

    @PutMapping("/admin/doctors/{id}")
    @RequiresRole({"ADMIN", "DOCTOR"})
    public ResponseEntity<ApiResponse<DoctorDto>> updateDoctor(@PathVariable Long id, @RequestBody UpdateDoctorRequest req) {
        Long actorId = SecurityContextHelper.getCurrentUserId();
        DoctorDto updated = doctorService.updateDoctor(id, req, actorId);
        return ResponseEntity.ok(ApiResponse.ok("Doctor updated successfully", updated));
    }

    @PatchMapping("/admin/doctors/{id}/approval")
    @RequiresRole("ADMIN")
    public ResponseEntity<ApiResponse<Void>> updateApproval(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long actorId = SecurityContextHelper.getCurrentUserId();
        String status = body.getOrDefault("status", "APPROVED");
        doctorService.updateApprovalStatus(id, status, actorId);
        return ResponseEntity.ok(ApiResponse.okMessage("Doctor approval status updated to " + status));
    }
}
