package com.healthcare.app.controller;

import com.healthcare.app.dto.ApiResponse;
import com.healthcare.app.dto.ScheduleDto;
import com.healthcare.app.dto.UpdateScheduleRequest;
import com.healthcare.app.entity.DoctorUnavailablePeriod;
import com.healthcare.app.security.RequiresRole;
import com.healthcare.app.security.SecurityContextHelper;
import com.healthcare.app.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping("/doctors/{id}/schedule")
    public ResponseEntity<ApiResponse<List<ScheduleDto>>> getDoctorSchedule(@PathVariable Long id) {
        List<ScheduleDto> schedules = scheduleService.getDoctorSchedules(id);
        return ResponseEntity.ok(ApiResponse.ok(schedules));
    }

    @PutMapping("/doctor/schedule")
    @RequiresRole("DOCTOR")
    public ResponseEntity<ApiResponse<Void>> updateSchedule(@Valid @RequestBody UpdateScheduleRequest req) {
        Long doctorId = SecurityContextHelper.getCurrentDoctorId();
        Long userId = SecurityContextHelper.getCurrentUserId();
        scheduleService.upsertSchedule(doctorId, req, userId);
        return ResponseEntity.ok(ApiResponse.okMessage("Working schedule updated successfully"));
    }

    @PostMapping("/doctor/leave")
    @RequiresRole("DOCTOR")
    public ResponseEntity<ApiResponse<Long>> requestLeave(@RequestBody Map<String, String> body) {
        Long doctorId = SecurityContextHelper.getCurrentDoctorId();
        Long userId = SecurityContextHelper.getCurrentUserId();

        LocalDateTime start = LocalDateTime.parse(body.get("startDatetime"));
        LocalDateTime end = LocalDateTime.parse(body.get("endDatetime"));
        String reason = body.getOrDefault("reason", "Doctor personal leave");

        Long id = scheduleService.requestLeave(doctorId, start, end, reason, userId);
        return ResponseEntity.ok(ApiResponse.ok("Leave registered successfully", id));
    }

    @GetMapping("/doctor/leaves")
    @RequiresRole("DOCTOR")
    public ResponseEntity<ApiResponse<List<DoctorUnavailablePeriod>>> getDoctorLeaves() {
        Long doctorId = SecurityContextHelper.getCurrentDoctorId();
        List<DoctorUnavailablePeriod> list = scheduleService.getDoctorUnavailablePeriods(doctorId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}
