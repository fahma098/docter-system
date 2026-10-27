package com.healthcare.app.service;

import com.healthcare.app.dto.*;
import com.healthcare.app.entity.*;
import com.healthcare.app.exception.BusinessValidationException;
import com.healthcare.app.exception.ResourceNotFoundException;
import com.healthcare.app.exception.SlotAlreadyBookedException;
import com.healthcare.app.exception.UnauthorizedException;
import com.healthcare.app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private UnavailablePeriodRepository unavailablePeriodRepository;

    @Autowired
    private AppointmentHistoryRepository historyRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public AppointmentDto createAppointment(Long patientId, CreateAppointmentRequest req, Long actorUserId) {
        // 1. Verify Patient
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient record not found with ID: " + patientId));

        // 2. Verify Doctor
        Doctor doctor = doctorRepository.findById(req.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor record not found with ID: " + req.getDoctorId()));

        if (!"APPROVED".equalsIgnoreCase(doctor.getApprovalStatus())) {
            throw new BusinessValidationException("Selected doctor is currently not accepting new bookings");
        }

        // 3. Validate Date
        LocalDate appDate = req.getAppointmentDate();
        LocalDate today = LocalDate.now();
        if (appDate.isBefore(today)) {
            throw new BusinessValidationException("Appointment date cannot be in the past");
        }

        int maxDays = Integer.parseInt(systemSettingRepository.getValueOrDefault("MAX_ADVANCE_BOOKING_DAYS", "60"));
        if (appDate.isAfter(today.plusDays(maxDays))) {
            throw new BusinessValidationException("Appointments cannot be booked more than " + maxDays + " days in advance");
        }

        // 4. Validate Minimum Advance Notice
        int minAdvanceHours = Integer.parseInt(systemSettingRepository.getValueOrDefault("MIN_ADVANCE_BOOKING_HOURS", "2"));
        LocalDateTime requestedStart = appDate.atTime(req.getStartTime());
        if (requestedStart.isBefore(LocalDateTime.now().plusHours(minAdvanceHours))) {
            throw new BusinessValidationException("Appointments must be booked at least " + minAdvanceHours + " hours in advance");
        }

        // 5. Validate Schedule & Working Hours
        String dayOfWeek = appDate.getDayOfWeek().name();
        DoctorSchedule schedule = scheduleRepository.findByDoctorAndDay(doctor.getDoctorId(), dayOfWeek)
                .orElseThrow(() -> new BusinessValidationException("Doctor does not consult on " + dayOfWeek));

        if (!schedule.isActiveStatus()) {
            throw new BusinessValidationException("Doctor schedule is inactive on " + dayOfWeek);
        }

        int duration = schedule.getSlotDurationMinutes() > 0 ? schedule.getSlotDurationMinutes() : doctor.getDefaultAppointmentDuration();
        LocalTime startTime = req.getStartTime();
        LocalTime endTime = startTime.plusMinutes(duration);

        if (startTime.isBefore(schedule.getStartTime()) || endTime.isAfter(schedule.getEndTime())) {
            throw new BusinessValidationException("Requested time is outside doctor's working hours (" +
                    schedule.getStartTime() + " - " + schedule.getEndTime() + ")");
        }

        // 6. Validate Break Time
        if (schedule.getBreakStartTime() != null && schedule.getBreakEndTime() != null) {
            if (startTime.isBefore(schedule.getBreakEndTime()) && endTime.isAfter(schedule.getBreakStartTime())) {
                throw new BusinessValidationException("Requested time overlaps with the doctor's scheduled break");
            }
        }

        // 7. Validate Leave / Unavailable Periods
        if (unavailablePeriodRepository.isDoctorUnavailable(doctor.getDoctorId(), requestedStart, appDate.atTime(endTime))) {
            throw new BusinessValidationException("Doctor is on approved leave during this time window");
        }

        // 8. DOUBLE-BOOKING PREVENTION CHECK (With Row Locking)
        boolean hasConflict = appointmentRepository.hasConflictingAppointmentWithLock(
                doctor.getDoctorId(), appDate, startTime, endTime, null);

        if (hasConflict) {
            throw new SlotAlreadyBookedException("The requested appointment slot (" + startTime + " - " + endTime + ") is no longer available. Please select another slot.");
        }

        // 9. Generate Unique Appointment Reference
        String reference = generateUniqueReference(appDate);

        // 10. Auto-confirm setting
        boolean autoConfirm = Boolean.parseBoolean(systemSettingRepository.getValueOrDefault("AUTO_CONFIRM_BOOKINGS", "true"));
        String initialStatus = autoConfirm ? "CONFIRMED" : "PENDING_CONFIRMATION";

        // 11. Create Appointment Entity
        Appointment appointment = new Appointment();
        appointment.setAppointmentReference(reference);
        appointment.setPatientId(patient.getPatientId());
        appointment.setDoctorId(doctor.getDoctorId());
        appointment.setAppointmentDate(appDate);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setAppointmentStatus(initialStatus);
        appointment.setConsultationFeeSnapshot(doctor.getConsultationFee());
        appointment.setAppointmentType(req.getAppointmentType() != null ? req.getAppointmentType() : "IN_PERSON");
        appointment.setPatientNotes(req.getPatientNotes());

        Long appointmentId = appointmentRepository.save(appointment);
        appointment.setAppointmentId(appointmentId);

        // 12. Record Initial Payment
        Payment payment = new Payment();
        payment.setAppointmentId(appointmentId);
        payment.setAmount(doctor.getConsultationFee());
        payment.setPaymentMethod("CREDIT_CARD");
        payment.setPaymentStatus("PAID");
        payment.setTransactionReference("TXN-" + reference.replace("DOC-", ""));
        paymentRepository.save(payment);

        // 13. Record Appointment History
        historyRepository.logHistory(appointmentId, actorUserId, "BOOKED", null, initialStatus, "Patient completed booking online");

        // 14. Audit Log
        auditLogRepository.log(actorUserId, "BOOK_APPOINTMENT", "APPOINTMENT", reference, "Booked appointment with Dr. " + doctor.getFullName());

        // 15. In-app notifications
        Notification patientNotification = new Notification();
        patientNotification.setUserId(patient.getUserId());
        patientNotification.setTitle("Appointment Confirmed (" + reference + ")");
        patientNotification.setMessage("Your consultation with " + doctor.getFullName() + " on " + appDate + " at " + startTime + " has been booked successfully.");
        patientNotification.setType("APPOINTMENT");
        notificationRepository.save(patientNotification);

        Notification doctorNotification = new Notification();
        doctorNotification.setUserId(doctor.getUserId());
        doctorNotification.setTitle("New Booking Received (" + reference + ")");
        doctorNotification.setMessage("Patient " + patient.getFullName() + " booked a consultation for " + appDate + " at " + startTime + ".");
        doctorNotification.setType("APPOINTMENT");
        notificationRepository.save(doctorNotification);

        return getAppointmentDtoById(appointmentId);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public AppointmentDto rescheduleAppointment(Long appointmentId, RescheduleRequest req, Long actorUserId, String actorRole) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // State check
        if ("COMPLETED".equals(appointment.getAppointmentStatus()) || "CANCELLED".equals(appointment.getAppointmentStatus())) {
            throw new BusinessValidationException("Cannot reschedule an appointment with status: " + appointment.getAppointmentStatus());
        }

        // Reschedule policy window check (e.g. at least 4 hours before existing appointment)
        if (!"ADMIN".equals(actorRole)) {
            int deadlineHours = Integer.parseInt(systemSettingRepository.getValueOrDefault("CANCELLATION_DEADLINE_HOURS", "4"));
            LocalDateTime currentAppointmentStart = appointment.getAppointmentDate().atTime(appointment.getStartTime());
            if (LocalDateTime.now().plusHours(deadlineHours).isAfter(currentAppointmentStart)) {
                throw new BusinessValidationException("Appointments cannot be rescheduled less than " + deadlineHours + " hours before the scheduled time");
            }
        }

        LocalDate newDate = req.getNewDate();
        if (newDate.isBefore(LocalDate.now())) {
            throw new BusinessValidationException("New appointment date cannot be in the past");
        }

        DoctorSchedule schedule = scheduleRepository.findByDoctorAndDay(appointment.getDoctorId(), newDate.getDayOfWeek().name())
                .orElseThrow(() -> new BusinessValidationException("Doctor does not consult on " + newDate.getDayOfWeek().name()));

        int duration = schedule.getSlotDurationMinutes() > 0 ? schedule.getSlotDurationMinutes() : 30;
        LocalTime newStart = req.getNewStartTime();
        LocalTime newEnd = newStart.plusMinutes(duration);

        if (newStart.isBefore(schedule.getStartTime()) || newEnd.isAfter(schedule.getEndTime())) {
            throw new BusinessValidationException("New time is outside doctor's working hours");
        }

        // Break check
        if (schedule.getBreakStartTime() != null && schedule.getBreakEndTime() != null) {
            if (newStart.isBefore(schedule.getBreakEndTime()) && newEnd.isAfter(schedule.getBreakStartTime())) {
                throw new BusinessValidationException("New time overlaps with doctor's scheduled break");
            }
        }

        // Conflict check with lock (excluding this appointment)
        boolean hasConflict = appointmentRepository.hasConflictingAppointmentWithLock(
                appointment.getDoctorId(), newDate, newStart, newEnd, appointmentId);

        if (hasConflict) {
            throw new SlotAlreadyBookedException("The requested slot is already booked. Please choose another time.");
        }

        // Update appointment
        appointmentRepository.reschedule(appointmentId, newDate, newStart, newEnd);

        // History
        String logReason = "Rescheduled from " + appointment.getAppointmentDate() + " " + appointment.getStartTime() + " to " + newDate + " " + newStart + ". Reason: " + (req.getReason() != null ? req.getReason() : "Patient requested");
        historyRepository.logHistory(appointmentId, actorUserId, "RESCHEDULED", appointment.getAppointmentStatus(), appointment.getAppointmentStatus(), logReason);
        auditLogRepository.log(actorUserId, "RESCHEDULE_APPOINTMENT", "APPOINTMENT", appointment.getAppointmentReference(), logReason);

        // Notify
        Notification patientNotification = new Notification();
        // find patient user ID
        Patient p = patientRepository.findById(appointment.getPatientId()).orElse(null);
        if (p != null) {
            patientNotification.setUserId(p.getUserId());
            patientNotification.setTitle("Appointment Rescheduled (" + appointment.getAppointmentReference() + ")");
            patientNotification.setMessage("Your consultation has been rescheduled to " + newDate + " at " + newStart + ".");
            patientNotification.setType("APPOINTMENT");
            notificationRepository.save(patientNotification);
        }

        return getAppointmentDtoById(appointmentId);
    }

    @Transactional
    public AppointmentDto cancelAppointment(Long appointmentId, CancelAppointmentRequest req, Long actorUserId, String actorRole) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        if ("COMPLETED".equals(appointment.getAppointmentStatus())) {
            throw new BusinessValidationException("Completed appointments cannot be cancelled");
        }
        if ("CANCELLED".equals(appointment.getAppointmentStatus())) {
            throw new BusinessValidationException("Appointment is already cancelled");
        }

        // Cancellation policy check
        if (!"ADMIN".equals(actorRole)) {
            int deadlineHours = Integer.parseInt(systemSettingRepository.getValueOrDefault("CANCELLATION_DEADLINE_HOURS", "4"));
            LocalDateTime currentStart = appointment.getAppointmentDate().atTime(appointment.getStartTime());
            if (LocalDateTime.now().plusHours(deadlineHours).isAfter(currentStart)) {
                throw new BusinessValidationException("Appointments cannot be cancelled within " + deadlineHours + " hours of the scheduled time. Please contact the clinic desk.");
            }
        }

        String reason = req != null && req.getReason() != null ? req.getReason().trim() : "Cancelled by user";
        appointmentRepository.updateStatus(appointmentId, "CANCELLED", reason, null);

        // Update payment if exists
        paymentRepository.updateStatus(appointmentId, "REFUNDED");

        // History
        historyRepository.logHistory(appointmentId, actorUserId, "CANCELLED", appointment.getAppointmentStatus(), "CANCELLED", reason);
        auditLogRepository.log(actorUserId, "CANCEL_APPOINTMENT", "APPOINTMENT", appointment.getAppointmentReference(), "Cancelled: " + reason);

        // Notify
        Patient p = patientRepository.findById(appointment.getPatientId()).orElse(null);
        if (p != null) {
            Notification n = new Notification();
            n.setUserId(p.getUserId());
            n.setTitle("Appointment Cancelled (" + appointment.getAppointmentReference() + ")");
            n.setMessage("Your appointment scheduled for " + appointment.getAppointmentDate() + " has been cancelled. Reason: " + reason);
            n.setType("APPOINTMENT");
            notificationRepository.save(n);
        }

        return getAppointmentDtoById(appointmentId);
    }

    @Transactional
    public AppointmentDto updateAppointmentStatus(Long appointmentId, UpdateStatusRequest req, Long actorUserId, String actorRole) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        String currentStatus = appointment.getAppointmentStatus();
        String nextStatus = req.getNewStatus().toUpperCase();

        // Enforce state machine transitions
        validateStatusTransition(currentStatus, nextStatus, actorRole);

        appointmentRepository.updateStatus(appointmentId, nextStatus, req.getReason(), req.getDoctorInstructions());
        historyRepository.logHistory(appointmentId, actorUserId, nextStatus, currentStatus, nextStatus, req.getReason());
        auditLogRepository.log(actorUserId, "UPDATE_STATUS", "APPOINTMENT", appointment.getAppointmentReference(), "Status changed from " + currentStatus + " to " + nextStatus);

        // Notify patient
        Patient p = patientRepository.findById(appointment.getPatientId()).orElse(null);
        if (p != null) {
            Notification n = new Notification();
            n.setUserId(p.getUserId());
            n.setTitle("Appointment Status Updated (" + appointment.getAppointmentReference() + ")");
            n.setMessage("Your appointment on " + appointment.getAppointmentDate() + " is now " + nextStatus + ".");
            n.setType("APPOINTMENT");
            notificationRepository.save(n);
        }

        return getAppointmentDtoById(appointmentId);
    }

    private void validateStatusTransition(String current, String next, String role) {
        if (current.equals(next)) {
            return;
        }

        if ("COMPLETED".equals(current) && !"ADMIN".equals(role)) {
            throw new BusinessValidationException("Completed appointments cannot change status");
        }
        if ("CANCELLED".equals(current) && !"ADMIN".equals(role)) {
            throw new BusinessValidationException("Cancelled appointments cannot be updated");
        }

        boolean valid = switch (current) {
            case "PENDING_CONFIRMATION" -> "CONFIRMED".equals(next) || "CANCELLED".equals(next);
            case "CONFIRMED" -> "CHECKED_IN".equals(next) || "CANCELLED".equals(next) || "NO_SHOW".equals(next) || "IN_PROGRESS".equals(next);
            case "CHECKED_IN" -> "IN_PROGRESS".equals(next) || "NO_SHOW".equals(next) || "CANCELLED".equals(next);
            case "IN_PROGRESS" -> "COMPLETED".equals(next);
            default -> "ADMIN".equals(role);
        };

        if (!valid && !"ADMIN".equals(role)) {
            throw new BusinessValidationException("Invalid status transition from " + current + " to " + next);
        }
    }

    public AppointmentDto getAppointmentDtoById(Long appointmentId) {
        Appointment a = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));
        return toDto(a);
    }

    public AppointmentDto getAppointmentDtoByReference(String reference) {
        Appointment a = appointmentRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with reference: " + reference));
        return toDto(a);
    }

    public List<AppointmentDto> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientId(patientId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<AppointmentDto> getDoctorAppointments(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<AppointmentDto> getAllAppointmentsWithFilters(Long doctorId, Long patientId, String status, LocalDate fromDate, LocalDate toDate) {
        return appointmentRepository.findAllWithFilters(doctorId, patientId, status, fromDate, toDate).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    private String generateUniqueReference(LocalDate date) {
        String datePart = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        byte[] randomBytes = new byte[3];
        secureRandom.nextBytes(randomBytes);
        StringBuilder sb = new StringBuilder("DOC-").append(datePart).append("-");
        for (byte b : randomBytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public AppointmentDto toDto(Appointment a) {
        AppointmentDto dto = new AppointmentDto();
        dto.setAppointmentId(a.getAppointmentId());
        dto.setAppointmentReference(a.getAppointmentReference());
        dto.setPatientId(a.getPatientId());
        dto.setPatientName(a.getPatientName());
        dto.setPatientEmail(a.getPatientEmail());
        dto.setPatientPhone(a.getPatientPhone());

        dto.setDoctorId(a.getDoctorId());
        dto.setDoctorName(a.getDoctorName());
        dto.setDoctorEmail(a.getDoctorEmail());
        dto.setDoctorPhone(a.getDoctorPhone());
        dto.setSpecializationName(a.getSpecializationName());
        dto.setDoctorQualifications(a.getDoctorQualifications());

        dto.setAppointmentDate(a.getAppointmentDate());
        dto.setStartTime(a.getStartTime());
        dto.setEndTime(a.getEndTime());
        dto.setAppointmentStatus(a.getAppointmentStatus());
        dto.setConsultationFee(a.getConsultationFeeSnapshot());
        dto.setAppointmentType(a.getAppointmentType());
        dto.setPatientNotes(a.getPatientNotes());
        dto.setCancellationReason(a.getCancellationReason());
        dto.setDoctorInstructions(a.getDoctorInstructions());

        dto.setPaymentStatus(a.getPaymentStatus());
        dto.setPaymentAmount(a.getPaymentAmount());
        dto.setPaymentMethod(a.getPaymentMethod());
        dto.setTransactionReference(a.getTransactionReference());

        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }
}
