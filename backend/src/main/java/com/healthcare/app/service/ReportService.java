package com.healthcare.app.service;

import com.healthcare.app.dto.AdminDashboardDto;
import com.healthcare.app.dto.AppointmentDto;
import com.healthcare.app.dto.DoctorDashboardDto;
import com.healthcare.app.dto.PatientDashboardDto;
import com.healthcare.app.entity.Appointment;
import com.healthcare.app.entity.Doctor;
import com.healthcare.app.entity.Patient;
import com.healthcare.app.exception.ResourceNotFoundException;
import com.healthcare.app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public AdminDashboardDto getAdminDashboardData() {
        AdminDashboardDto dto = new AdminDashboardDto();
        LocalDate today = LocalDate.now();

        dto.setTotalDoctors(doctorRepository.countTotalDoctors());
        dto.setTotalPatients(patientRepository.countTotalPatients());
        dto.setTodayAppointments(appointmentRepository.countTodayAppointments(today));
        dto.setPendingAppointments(appointmentRepository.countByStatus("PENDING_CONFIRMATION"));
        dto.setCompletedAppointments(appointmentRepository.countByStatus("COMPLETED"));
        dto.setCancelledAppointments(appointmentRepository.countByStatus("CANCELLED"));
        dto.setTotalRevenueCollected(paymentRepository.sumPaidRevenue());

        // Recent appointments
        List<AppointmentDto> recent = appointmentRepository.findAllWithFilters(null, null, null, null, null)
                .stream().limit(10).map(appointmentService::toDto).collect(Collectors.toList());
        dto.setRecentAppointments(recent);

        // Status breakdown
        Map<String, Integer> statusMap = new LinkedHashMap<>();
        String[] statuses = {"CONFIRMED", "PENDING_CONFIRMATION", "CHECKED_IN", "IN_PROGRESS", "COMPLETED", "CANCELLED", "NO_SHOW"};
        for (String s : statuses) {
            statusMap.put(s, appointmentRepository.countByStatus(s));
        }
        dto.setAppointmentsByStatus(statusMap);

        // Specialization breakdown
        Map<String, Integer> specMap = new LinkedHashMap<>();
        String specSql = """
            SELECT s.specialization_name, COUNT(a.appointment_id) AS cnt
            FROM specializations s
            LEFT JOIN doctors d ON s.specialization_id = d.specialization_id
            LEFT JOIN appointments a ON d.doctor_id = a.doctor_id
            GROUP BY s.specialization_name
            ORDER BY cnt DESC
        """;
        jdbcTemplate.query(specSql, rs -> {
            specMap.put(rs.getString("specialization_name"), rs.getInt("cnt"));
        });
        dto.setAppointmentsBySpecialization(specMap);

        // Last 7 days breakdown
        Map<String, Integer> last7Days = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String dayLabel = d.format(DateTimeFormatter.ofPattern("MMM dd"));
            int count = appointmentRepository.countTodayAppointments(d);
            last7Days.put(dayLabel, count);
        }
        dto.setAppointmentsLast7Days(last7Days);

        return dto;
    }

    public DoctorDashboardDto getDoctorDashboardData(Long doctorId) {
        Doctor doc = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        DoctorDashboardDto dto = new DoctorDashboardDto();
        dto.setDoctorName(doc.getFullName());
        dto.setSpecialization(doc.getSpecializationName());

        LocalDate today = LocalDate.now();
        dto.setTodayCount(appointmentRepository.countDoctorTodayAppointments(doctorId, today));
        dto.setUpcomingCount(appointmentRepository.countDoctorUpcomingAppointments(doctorId, today));
        dto.setCompletedTodayCount(appointmentRepository.countDoctorCompletedToday(doctorId, today));
        dto.setPendingCount(appointmentRepository.countDoctorPendingConfirmations(doctorId));
        dto.setTotalPatients(appointmentRepository.countDoctorUniquePatients(doctorId));
        dto.setMonthlyEarnings(appointmentRepository.sumDoctorMonthlyEarnings(doctorId, today.getYear(), today.getMonthValue()));

        List<AppointmentDto> todayApps = appointmentRepository.findDoctorTodayAppointments(doctorId, today)
                .stream().map(appointmentService::toDto).collect(Collectors.toList());
        dto.setTodayAppointments(todayApps);

        List<AppointmentDto> upcoming = appointmentRepository.findByDoctorId(doctorId).stream()
                .filter(a -> !a.getAppointmentDate().isBefore(today) && !"CANCELLED".equals(a.getAppointmentStatus()))
                .limit(15)
                .map(appointmentService::toDto).collect(Collectors.toList());
        dto.setUpcomingAppointments(upcoming);

        dto.setCurrentSchedules(scheduleService.getDoctorSchedules(doctorId));

        return dto;
    }

    public PatientDashboardDto getPatientDashboardData(Long patientId) {
        Patient p = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));

        PatientDashboardDto dto = new PatientDashboardDto();
        dto.setPatientName(p.getFullName());

        List<Appointment> all = appointmentRepository.findByPatientId(patientId);
        LocalDate today = LocalDate.now();

        List<AppointmentDto> upcoming = new ArrayList<>();
        List<AppointmentDto> past = new ArrayList<>();

        int upCount = 0, compCount = 0, pendCount = 0, cancCount = 0;

        for (Appointment a : all) {
            AppointmentDto ad = appointmentService.toDto(a);
            if ("COMPLETED".equals(a.getAppointmentStatus())) compCount++;
            else if ("PENDING_CONFIRMATION".equals(a.getAppointmentStatus())) pendCount++;
            else if ("CANCELLED".equals(a.getAppointmentStatus())) cancCount++;

            if ((a.getAppointmentDate().isAfter(today) || a.getAppointmentDate().isEqual(today))
                    && !"CANCELLED".equals(a.getAppointmentStatus()) && !"COMPLETED".equals(a.getAppointmentStatus())) {
                upcoming.add(ad);
                upCount++;
            } else {
                past.add(ad);
            }
        }

        dto.setUpcomingCount(upCount);
        dto.setCompletedCount(compCount);
        dto.setPendingCount(pendCount);
        dto.setCancelledCount(cancCount);
        dto.setUpcomingAppointments(upcoming);
        dto.setPastAppointments(past);

        return dto;
    }

    public byte[] exportAppointmentsCsv(Long doctorId, Long patientId, String status, LocalDate fromDate, LocalDate toDate) {
        List<AppointmentDto> list = appointmentService.getAllAppointmentsWithFilters(doctorId, patientId, status, fromDate, toDate);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            // CSV Header
            writer.println("Reference,Patient Name,Patient Email,Patient Phone,Doctor Name,Specialization,Date,Time,Type,Status,Fee,Payment Status");

            for (AppointmentDto a : list) {
                writer.print(escapeCsv(a.getAppointmentReference())); writer.print(",");
                writer.print(escapeCsv(a.getPatientName())); writer.print(",");
                writer.print(escapeCsv(a.getPatientEmail())); writer.print(",");
                writer.print(escapeCsv(a.getPatientPhone())); writer.print(",");
                writer.print(escapeCsv(a.getDoctorName())); writer.print(",");
                writer.print(escapeCsv(a.getSpecializationName())); writer.print(",");
                writer.print(escapeCsv(String.valueOf(a.getAppointmentDate()))); writer.print(",");
                writer.print(escapeCsv(a.getStartTime() + " - " + a.getEndTime())); writer.print(",");
                writer.print(escapeCsv(a.getAppointmentType())); writer.print(",");
                writer.print(escapeCsv(a.getAppointmentStatus())); writer.print(",");
                writer.print(a.getConsultationFee() != null ? a.getConsultationFee() : BigDecimal.ZERO); writer.print(",");
                writer.println(escapeCsv(a.getPaymentStatus() != null ? a.getPaymentStatus() : "PENDING"));
            }
        }
        return out.toByteArray();
    }

    private String escapeCsv(String input) {
        if (input == null) return "\"\"";
        String val = input.trim();
        // Prevent CSV Formula Injection
        if (val.startsWith("=") || val.startsWith("+") || val.startsWith("-") || val.startsWith("@") || val.startsWith("\t") || val.startsWith("\r")) {
            val = "'" + val;
        }
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}
