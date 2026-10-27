package com.healthcare.app.controller;

import com.healthcare.app.dto.ApiResponse;
import com.healthcare.app.dto.AppointmentDto;
import com.healthcare.app.dto.DoctorDashboardDto;
import com.healthcare.app.security.RequiresRole;
import com.healthcare.app.security.SecurityContextHelper;
import com.healthcare.app.service.AppointmentService;
import com.healthcare.app.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
public class DoctorDashboardController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping("/dashboard")
    @RequiresRole("DOCTOR")
    public ResponseEntity<ApiResponse<DoctorDashboardDto>> getDashboard() {
        Long doctorId = SecurityContextHelper.getCurrentDoctorId();
        DoctorDashboardDto data = reportService.getDoctorDashboardData(doctorId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/appointments")
    @RequiresRole("DOCTOR")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getDoctorAppointments() {
        Long doctorId = SecurityContextHelper.getCurrentDoctorId();
        List<AppointmentDto> list = appointmentService.getDoctorAppointments(doctorId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}
