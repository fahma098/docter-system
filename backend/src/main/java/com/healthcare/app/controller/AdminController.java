package com.healthcare.app.controller;

import com.healthcare.app.dto.AdminDashboardDto;
import com.healthcare.app.dto.ApiResponse;
import com.healthcare.app.dto.AppointmentDto;
import com.healthcare.app.entity.AuditLog;
import com.healthcare.app.entity.Patient;
import com.healthcare.app.repository.AuditLogRepository;
import com.healthcare.app.repository.PatientRepository;
import com.healthcare.app.repository.SystemSettingRepository;
import com.healthcare.app.security.RequiresRole;
import com.healthcare.app.security.SecurityContextHelper;
import com.healthcare.app.service.AppointmentService;
import com.healthcare.app.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiresRole("ADMIN")
public class AdminController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardDto>> getDashboard() {
        AdminDashboardDto dto = reportService.getAdminDashboardData();
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getAppointments(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        List<AppointmentDto> list = appointmentService.getAllAppointmentsWithFilters(doctorId, patientId, status, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/patients")
    public ResponseEntity<ApiResponse<List<Patient>>> getPatients() {
        List<Patient> list = patientRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogs(@RequestParam(defaultValue = "50") int limit) {
        List<AuditLog> list = auditLogRepository.findRecent(limit);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSettings() {
        Map<String, String> settings = systemSettingRepository.getAllSettings();
        return ResponseEntity.ok(ApiResponse.ok(settings));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<Void>> updateSetting(@RequestBody Map<String, String> body) {
        Long userId = SecurityContextHelper.getCurrentUserId();
        String key = body.get("key");
        String value = body.get("value");
        String desc = body.getOrDefault("description", "Updated via Admin Console");

        systemSettingRepository.setSetting(key, value, desc);
        auditLogRepository.log(userId, "UPDATE_SETTING", "SYSTEM_SETTING", key, "Changed setting " + key + " to " + value);
        return ResponseEntity.ok(ApiResponse.okMessage("System setting updated successfully"));
    }

    @GetMapping("/reports/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        byte[] csv = reportService.exportAppointmentsCsv(doctorId, patientId, status, fromDate, toDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "appointments_report_" + LocalDate.now() + ".csv");

        return ResponseEntity.ok().headers(headers).body(csv);
    }
}
