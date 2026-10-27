package com.healthcare.app.controller;

import com.healthcare.app.dto.*;
import com.healthcare.app.exception.UnauthorizedException;
import com.healthcare.app.security.RequiresRole;
import com.healthcare.app.security.SecurityContextHelper;
import com.healthcare.app.security.SessionTokenManager;
import com.healthcare.app.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @PostMapping
    @RequiresRole("PATIENT")
    public ResponseEntity<ApiResponse<AppointmentDto>> bookAppointment(@Valid @RequestBody CreateAppointmentRequest req) {
        Long patientId = SecurityContextHelper.getCurrentPatientId();
        Long actorUserId = SecurityContextHelper.getCurrentUserId();
        AppointmentDto created = appointmentService.createAppointment(patientId, req, actorUserId);
        return ResponseEntity.ok(ApiResponse.ok("Appointment successfully confirmed", created));
    }

    @GetMapping("/my")
    @RequiresRole("PATIENT")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getMyAppointments() {
        Long patientId = SecurityContextHelper.getCurrentPatientId();
        List<AppointmentDto> list = appointmentService.getPatientAppointments(patientId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    @RequiresRole({"PATIENT", "DOCTOR", "ADMIN"})
    public ResponseEntity<ApiResponse<AppointmentDto>> getAppointmentById(@PathVariable Long id) {
        SessionTokenManager.UserSession session = SecurityContextHelper.getCurrentSession();
        AppointmentDto dto = appointmentService.getAppointmentDtoById(id);

        // Security authorization check: patients can only access their own appointments
        if ("PATIENT".equals(session.getRole()) && !session.getPatientId().equals(dto.getPatientId())) {
            throw new UnauthorizedException("You are not authorized to view this appointment");
        }
        if ("DOCTOR".equals(session.getRole()) && !session.getDoctorId().equals(dto.getDoctorId())) {
            throw new UnauthorizedException("You are not authorized to view this appointment");
        }

        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/verify/{ref}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyAppointmentReceipt(@PathVariable String ref) {
        // Public endpoint for QR code validation (shows only safe verification metadata)
        AppointmentDto dto = appointmentService.getAppointmentDtoByReference(ref);
        Map<String, Object> verificationData = new HashMap<>();
        verificationData.put("valid", true);
        verificationData.put("reference", dto.getAppointmentReference());
        verificationData.put("doctorName", dto.getDoctorName());
        verificationData.put("specialization", dto.getSpecializationName());
        verificationData.put("appointmentDate", dto.getAppointmentDate());
        verificationData.put("timeSlot", dto.getStartTime() + " - " + dto.getEndTime());
        verificationData.put("status", dto.getAppointmentStatus());
        verificationData.put("paymentStatus", dto.getPaymentStatus());
        return ResponseEntity.ok(ApiResponse.ok("Valid official appointment verified", verificationData));
    }

    @PostMapping("/{id}/reschedule")
    @RequiresRole({"PATIENT", "DOCTOR", "ADMIN"})
    public ResponseEntity<ApiResponse<AppointmentDto>> rescheduleAppointment(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleRequest req) {
        SessionTokenManager.UserSession session = SecurityContextHelper.getCurrentSession();
        AppointmentDto dto = appointmentService.getAppointmentDtoById(id);

        if ("PATIENT".equals(session.getRole()) && !session.getPatientId().equals(dto.getPatientId())) {
            throw new UnauthorizedException("You are not authorized to reschedule this appointment");
        }
        if ("DOCTOR".equals(session.getRole()) && !session.getDoctorId().equals(dto.getDoctorId())) {
            throw new UnauthorizedException("You are not authorized to reschedule this appointment");
        }

        AppointmentDto updated = appointmentService.rescheduleAppointment(id, req, session.getUserId(), session.getRole());
        return ResponseEntity.ok(ApiResponse.ok("Appointment successfully rescheduled", updated));
    }

    @PostMapping("/{id}/cancel")
    @RequiresRole({"PATIENT", "DOCTOR", "ADMIN"})
    public ResponseEntity<ApiResponse<AppointmentDto>> cancelAppointment(
            @PathVariable Long id,
            @RequestBody(required = false) CancelAppointmentRequest req) {
        SessionTokenManager.UserSession session = SecurityContextHelper.getCurrentSession();
        AppointmentDto dto = appointmentService.getAppointmentDtoById(id);

        if ("PATIENT".equals(session.getRole()) && !session.getPatientId().equals(dto.getPatientId())) {
            throw new UnauthorizedException("You are not authorized to cancel this appointment");
        }

        AppointmentDto updated = appointmentService.cancelAppointment(id, req, session.getUserId(), session.getRole());
        return ResponseEntity.ok(ApiResponse.ok("Appointment cancelled", updated));
    }

    @PatchMapping("/{id}/status")
    @RequiresRole({"DOCTOR", "ADMIN"})
    public ResponseEntity<ApiResponse<AppointmentDto>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest req) {
        SessionTokenManager.UserSession session = SecurityContextHelper.getCurrentSession();
        AppointmentDto dto = appointmentService.getAppointmentDtoById(id);

        if ("DOCTOR".equals(session.getRole()) && !session.getDoctorId().equals(dto.getDoctorId())) {
            throw new UnauthorizedException("You are not authorized to update this appointment");
        }

        AppointmentDto updated = appointmentService.updateAppointmentStatus(id, req, session.getUserId(), session.getRole());
        return ResponseEntity.ok(ApiResponse.ok("Status updated to " + req.getNewStatus(), updated));
    }
}
