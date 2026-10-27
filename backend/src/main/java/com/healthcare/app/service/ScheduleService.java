package com.healthcare.app.service;

import com.healthcare.app.dto.ScheduleDto;
import com.healthcare.app.dto.UpdateScheduleRequest;
import com.healthcare.app.entity.DoctorSchedule;
import com.healthcare.app.entity.DoctorUnavailablePeriod;
import com.healthcare.app.exception.BusinessValidationException;
import com.healthcare.app.repository.AuditLogRepository;
import com.healthcare.app.repository.ScheduleRepository;
import com.healthcare.app.repository.UnavailablePeriodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private UnavailablePeriodRepository unavailablePeriodRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    public List<ScheduleDto> getDoctorSchedules(Long doctorId) {
        return scheduleRepository.findByDoctorId(doctorId).stream().map(s -> {
            ScheduleDto dto = new ScheduleDto();
            dto.setScheduleId(s.getScheduleId());
            dto.setDoctorId(s.getDoctorId());
            dto.setWorkingDay(s.getWorkingDay());
            dto.setStartTime(s.getStartTime());
            dto.setEndTime(s.getEndTime());
            dto.setBreakStartTime(s.getBreakStartTime());
            dto.setBreakEndTime(s.getBreakEndTime());
            dto.setSlotDurationMinutes(s.getSlotDurationMinutes());
            dto.setActiveStatus(s.isActiveStatus());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void upsertSchedule(Long doctorId, UpdateScheduleRequest req, Long actorUserId) {
        if (req.getStartTime().isAfter(req.getEndTime()) || req.getStartTime().equals(req.getEndTime())) {
            throw new BusinessValidationException("Start time must be before end time");
        }

        if (req.getBreakStartTime() != null && req.getBreakEndTime() != null) {
            if (req.getBreakStartTime().isBefore(req.getStartTime()) || req.getBreakEndTime().isAfter(req.getEndTime())) {
                throw new BusinessValidationException("Break period must be inside working hours");
            }
            if (!req.getBreakStartTime().isBefore(req.getBreakEndTime())) {
                throw new BusinessValidationException("Break start time must be before break end time");
            }
        }

        Optional<DoctorSchedule> existing = scheduleRepository.findByDoctorAndDay(doctorId, req.getWorkingDay());
        if (existing.isPresent()) {
            DoctorSchedule s = existing.get();
            s.setStartTime(req.getStartTime());
            s.setEndTime(req.getEndTime());
            s.setBreakStartTime(req.getBreakStartTime());
            s.setBreakEndTime(req.getBreakEndTime());
            s.setSlotDurationMinutes(req.getSlotDurationMinutes());
            s.setActiveStatus(req.isActiveStatus());
            scheduleRepository.update(s);
        } else {
            DoctorSchedule s = new DoctorSchedule();
            s.setDoctorId(doctorId);
            s.setWorkingDay(req.getWorkingDay().toUpperCase());
            s.setStartTime(req.getStartTime());
            s.setEndTime(req.getEndTime());
            s.setBreakStartTime(req.getBreakStartTime());
            s.setBreakEndTime(req.getBreakEndTime());
            s.setSlotDurationMinutes(req.getSlotDurationMinutes());
            s.setActiveStatus(req.isActiveStatus());
            scheduleRepository.save(s);
        }

        auditLogRepository.log(actorUserId, "UPDATE_SCHEDULE", "SCHEDULE", String.valueOf(doctorId), "Updated schedule for " + req.getWorkingDay());
    }

    @Transactional
    public Long requestLeave(Long doctorId, LocalDateTime start, LocalDateTime end, String reason, Long actorUserId) {
        if (!start.isBefore(end)) {
            throw new BusinessValidationException("Leave start date must be before end date");
        }

        DoctorUnavailablePeriod period = new DoctorUnavailablePeriod();
        period.setDoctorId(doctorId);
        period.setStartDatetime(start);
        period.setEndDatetime(end);
        period.setReason(reason != null ? reason : "Doctor personal/professional leave");
        period.setApprovalStatus("APPROVED"); // Auto-approved for verified doctors

        Long id = unavailablePeriodRepository.save(period);
        auditLogRepository.log(actorUserId, "REQUEST_LEAVE", "UNAVAILABLE_PERIOD", String.valueOf(id), "Doctor requested leave: " + reason);
        return id;
    }

    public List<DoctorUnavailablePeriod> getDoctorUnavailablePeriods(Long doctorId) {
        return unavailablePeriodRepository.findByDoctorId(doctorId);
    }
}
