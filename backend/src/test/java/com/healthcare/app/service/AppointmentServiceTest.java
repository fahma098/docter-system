package com.healthcare.app.service;

import com.healthcare.app.dto.CreateAppointmentRequest;
import com.healthcare.app.dto.DayAvailabilityDto;
import com.healthcare.app.dto.RescheduleRequest;
import com.healthcare.app.dto.UpdateStatusRequest;
import com.healthcare.app.entity.Appointment;
import com.healthcare.app.entity.Doctor;
import com.healthcare.app.entity.DoctorSchedule;
import com.healthcare.app.entity.Patient;
import com.healthcare.app.exception.BusinessValidationException;
import com.healthcare.app.exception.SlotAlreadyBookedException;
import com.healthcare.app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private UnavailablePeriodRepository unavailablePeriodRepository;

    @Mock
    private AppointmentHistoryRepository historyRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private Patient testPatient;
    private Doctor testDoctor;
    private DoctorSchedule testSchedule;

    @BeforeEach
    void setUp() {
        testPatient = new Patient();
        testPatient.setPatientId(1L);
        testPatient.setUserId(10L);
        testPatient.setFullName("John Doe");

        testDoctor = new Doctor();
        testDoctor.setDoctorId(2L);
        testDoctor.setUserId(20L);
        testDoctor.setFullName("Dr. Sarah Jenkins");
        testDoctor.setApprovalStatus("APPROVED");
        testDoctor.setConsultationFee(new BigDecimal("120.00"));
        testDoctor.setDefaultAppointmentDuration(30);

        testSchedule = new DoctorSchedule();
        testSchedule.setScheduleId(1L);
        testSchedule.setDoctorId(2L);
        testSchedule.setWorkingDay("MONDAY");
        testSchedule.setStartTime(LocalTime.of(9, 0));
        testSchedule.setEndTime(LocalTime.of(17, 0));
        testSchedule.setBreakStartTime(LocalTime.of(13, 0));
        testSchedule.setBreakEndTime(LocalTime.of(14, 0));
        testSchedule.setSlotDurationMinutes(30);
        testSchedule.setActiveStatus(true);
    }

    @Test
    @DisplayName("Should prevent double booking when slot is already booked")
    void shouldPreventDoubleBooking() {
        LocalDate nextMonday = LocalDate.now().plusDays(7);
        while (!"MONDAY".equalsIgnoreCase(nextMonday.getDayOfWeek().name())) {
            nextMonday = nextMonday.plusDays(1);
        }

        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setDoctorId(2L);
        request.setAppointmentDate(nextMonday);
        request.setStartTime(LocalTime.of(10, 0));

        when(patientRepository.findById(1L)).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(testDoctor));
        when(systemSettingRepository.getValueOrDefault("MAX_ADVANCE_BOOKING_DAYS", "60")).thenReturn("60");
        when(systemSettingRepository.getValueOrDefault("MIN_ADVANCE_BOOKING_HOURS", "2")).thenReturn("2");
        when(scheduleRepository.findByDoctorAndDay(eq(2L), anyString())).thenReturn(Optional.of(testSchedule));
        when(unavailablePeriodRepository.isDoctorUnavailable(anyLong(), any(), any())).thenReturn(false);

        // Simulate slot conflict detected by row-locking query
        when(appointmentRepository.hasConflictingAppointmentWithLock(eq(2L), eq(nextMonday), any(), any(), isNull()))
                .thenReturn(true);

        assertThrows(SlotAlreadyBookedException.class, () -> {
            appointmentService.createAppointment(1L, request, 10L);
        });

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject appointment request in the past")
    void shouldRejectPastDate() {
        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setDoctorId(2L);
        request.setAppointmentDate(LocalDate.now().minusDays(1));
        request.setStartTime(LocalTime.of(10, 0));

        when(patientRepository.findById(1L)).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(testDoctor));

        assertThrows(BusinessValidationException.class, () -> {
            appointmentService.createAppointment(1L, request, 10L);
        });
    }

    @Test
    @DisplayName("Should reject status transition from COMPLETED to CANCELLED")
    void shouldRejectInvalidStatusTransition() {
        Appointment completedApp = new Appointment();
        completedApp.setAppointmentId(99L);
        completedApp.setAppointmentStatus("COMPLETED");

        when(appointmentRepository.findById(99L)).thenReturn(Optional.of(completedApp));

        UpdateStatusRequest req = new UpdateStatusRequest("CANCELLED", "Cannot come", null);

        assertThrows(BusinessValidationException.class, () -> {
            appointmentService.updateAppointmentStatus(99L, req, 20L, "DOCTOR");
        });
    }
}
