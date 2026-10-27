package com.healthcare.app.service;

import com.healthcare.app.dto.AvailableSlotDto;
import com.healthcare.app.dto.DayAvailabilityDto;
import com.healthcare.app.entity.Appointment;
import com.healthcare.app.entity.Doctor;
import com.healthcare.app.entity.DoctorSchedule;
import com.healthcare.app.exception.ResourceNotFoundException;
import com.healthcare.app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AvailabilityService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private UnavailablePeriodRepository unavailablePeriodRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    public DayAvailabilityDto getDoctorAvailabilityForDate(Long doctorId, LocalDate date) {
        Doctor doc = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        DayAvailabilityDto response = new DayAvailabilityDto();
        response.setDoctorId(doctorId);
        response.setDoctorName(doc.getFullName());
        response.setDate(date);
        response.setDayOfWeek(date.getDayOfWeek().name());

        List<AvailableSlotDto> slots = new ArrayList<>();
        response.setSlots(slots);

        // 1. Check if date is in the past
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            response.setWorkingDay(false);
            response.setOnLeave(false);
            response.setTotalSlots(0);
            response.setAvailableSlotsCount(0);
            return response;
        }

        // 2. Check maximum advance booking window
        int maxDays = Integer.parseInt(systemSettingRepository.getValueOrDefault("MAX_ADVANCE_BOOKING_DAYS", "60"));
        if (date.isAfter(today.plusDays(maxDays))) {
            response.setWorkingDay(false);
            response.setOnLeave(false);
            response.setTotalSlots(0);
            response.setAvailableSlotsCount(0);
            return response;
        }

        // 3. Check if doctor is on approved leave for the entire day or overlaps
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(23, 59, 59);
        boolean isWholeDayLeave = unavailablePeriodRepository.isDoctorUnavailable(doctorId, dayStart, dayEnd);
        if (isWholeDayLeave) {
            response.setOnLeave(true);
            response.setWorkingDay(true);
            response.setTotalSlots(0);
            response.setAvailableSlotsCount(0);
            return response;
        }

        // 4. Check doctor's schedule for that day of week
        String dayOfWeekStr = date.getDayOfWeek().name();
        Optional<DoctorSchedule> scheduleOpt = scheduleRepository.findByDoctorAndDay(doctorId, dayOfWeekStr);
        if (scheduleOpt.isEmpty() || !scheduleOpt.get().isActiveStatus()) {
            response.setWorkingDay(false);
            response.setOnLeave(false);
            response.setTotalSlots(0);
            response.setAvailableSlotsCount(0);
            return response;
        }

        DoctorSchedule schedule = scheduleOpt.get();
        response.setWorkingDay(true);
        response.setOnLeave(false);

        int durationMinutes = schedule.getSlotDurationMinutes() > 0 ? schedule.getSlotDurationMinutes() : 30;
        LocalTime current = schedule.getStartTime();
        LocalTime end = schedule.getEndTime();
        LocalTime breakStart = schedule.getBreakStartTime();
        LocalTime breakEnd = schedule.getBreakEndTime();

        // Existing appointments for the day
        List<Appointment> existingAppointments = appointmentRepository.findByDoctorAndDate(doctorId, date);

        int minAdvanceHours = Integer.parseInt(systemSettingRepository.getValueOrDefault("MIN_ADVANCE_BOOKING_HOURS", "0"));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minAdvanceDateTime = minAdvanceHours > 0 ? now.plusHours(minAdvanceHours) : now.minusMinutes(5);

        int availableCount = 0;

        while (current.plusMinutes(durationMinutes).isBefore(end) || current.plusMinutes(durationMinutes).equals(end)) {
            LocalTime slotEnd = current.plusMinutes(durationMinutes);
            LocalDateTime slotStartDateTime = date.atTime(current);
            LocalDateTime slotEndDateTime = date.atTime(slotEnd);

            boolean isAvailable = true;
            String reason = null;

            // Past slot check (if date is today)
            if (date.isEqual(today) && slotStartDateTime.isBefore(minAdvanceDateTime)) {
                isAvailable = false;
                reason = "PAST_OR_TOO_SOON";
            }

            // Break check
            if (isAvailable && breakStart != null && breakEnd != null) {
                if ((current.isBefore(breakEnd) && slotEnd.isAfter(breakStart))) {
                    isAvailable = false;
                    reason = "BREAK";
                }
            }

            // Leave check for specific slot time
            if (isAvailable && unavailablePeriodRepository.isDoctorUnavailable(doctorId, slotStartDateTime, slotEndDateTime)) {
                isAvailable = false;
                reason = "LEAVE";
            }

            // Existing appointment conflict check
            if (isAvailable) {
                for (Appointment app : existingAppointments) {
                    if ((current.isBefore(app.getEndTime()) && slotEnd.isAfter(app.getStartTime()))) {
                        isAvailable = false;
                        reason = "BOOKED";
                        break;
                    }
                }
            }

            String formattedSlot = current.format(TIME_FORMATTER) + " - " + slotEnd.format(TIME_FORMATTER);
            slots.add(new AvailableSlotDto(current, slotEnd, formattedSlot, isAvailable, reason));

            if (isAvailable) {
                availableCount++;
            }

            current = slotEnd;
        }

        response.setTotalSlots(slots.size());
        response.setAvailableSlotsCount(availableCount);
        return response;
    }
}
