package com.healthcare.app.repository;

import com.healthcare.app.entity.Appointment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AppointmentRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Appointment> appointmentRowMapper = (rs, rowNum) -> {
        Appointment a = new Appointment();
        a.setAppointmentId(rs.getLong("appointment_id"));
        a.setAppointmentReference(rs.getString("appointment_reference"));
        a.setPatientId(rs.getLong("patient_id"));
        a.setDoctorId(rs.getLong("doctor_id"));
        if (rs.getDate("appointment_date") != null) {
            a.setAppointmentDate(rs.getDate("appointment_date").toLocalDate());
        }
        if (rs.getTime("start_time") != null) {
            a.setStartTime(rs.getTime("start_time").toLocalTime());
        }
        if (rs.getTime("end_time") != null) {
            a.setEndTime(rs.getTime("end_time").toLocalTime());
        }
        a.setAppointmentStatus(rs.getString("appointment_status"));
        a.setConsultationFeeSnapshot(rs.getBigDecimal("consultation_fee_snapshot"));
        a.setAppointmentType(rs.getString("appointment_type"));
        a.setPatientNotes(rs.getString("patient_notes"));
        a.setCancellationReason(rs.getString("cancellation_reason"));
        a.setDoctorInstructions(rs.getString("doctor_instructions"));
        if (rs.getTimestamp("created_at") != null) {
            a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("updated_at") != null) {
            a.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }

        // Joined columns
        try {
            a.setPatientName(rs.getString("patient_name"));
            a.setPatientEmail(rs.getString("patient_email"));
            a.setPatientPhone(rs.getString("patient_phone"));
            a.setDoctorName(rs.getString("doctor_name"));
            a.setDoctorEmail(rs.getString("doctor_email"));
            a.setDoctorPhone(rs.getString("doctor_phone"));
            a.setSpecializationName(rs.getString("specialization_name"));
            a.setDoctorQualifications(rs.getString("doctor_qualifications"));
            a.setPaymentStatus(rs.getString("payment_status"));
            a.setPaymentAmount(rs.getBigDecimal("payment_amount"));
            a.setPaymentMethod(rs.getString("payment_method"));
            a.setTransactionReference(rs.getString("transaction_reference"));
        } catch (Exception ignored) {}

        return a;
    };

    private static final String BASE_APPOINTMENT_QUERY = """
        SELECT a.*, 
               pu.full_name AS patient_name, pu.email AS patient_email, pu.phone AS patient_phone,
               du.full_name AS doctor_name, du.email AS doctor_email, du.phone AS doctor_phone,
               d.qualifications AS doctor_qualifications,
               s.specialization_name,
               pay.payment_status, pay.amount AS payment_amount, pay.payment_method, pay.transaction_reference
        FROM appointments a
        JOIN patients p ON a.patient_id = p.patient_id
        JOIN users pu ON p.user_id = pu.user_id
        JOIN doctors d ON a.doctor_id = d.doctor_id
        JOIN users du ON d.user_id = du.user_id
        JOIN specializations s ON d.specialization_id = s.specialization_id
        LEFT JOIN payments pay ON a.appointment_id = pay.appointment_id
    """;

    public Optional<Appointment> findById(Long appointmentId) {
        String sql = BASE_APPOINTMENT_QUERY + " WHERE a.appointment_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, appointmentRowMapper, appointmentId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Appointment> findByReference(String reference) {
        String sql = BASE_APPOINTMENT_QUERY + " WHERE a.appointment_reference = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, appointmentRowMapper, reference));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Appointment> findByPatientId(Long patientId) {
        String sql = BASE_APPOINTMENT_QUERY + " WHERE a.patient_id = ? ORDER BY a.appointment_date DESC, a.start_time DESC";
        return jdbcTemplate.query(sql, appointmentRowMapper, patientId);
    }

    public List<Appointment> findByDoctorId(Long doctorId) {
        String sql = BASE_APPOINTMENT_QUERY + " WHERE a.doctor_id = ? ORDER BY a.appointment_date DESC, a.start_time DESC";
        return jdbcTemplate.query(sql, appointmentRowMapper, doctorId);
    }

    public List<Appointment> findByDoctorAndDate(Long doctorId, LocalDate date) {
        String sql = BASE_APPOINTMENT_QUERY + " WHERE a.doctor_id = ? AND a.appointment_date = ? AND a.appointment_status NOT IN ('CANCELLED') ORDER BY a.start_time ASC";
        return jdbcTemplate.query(sql, appointmentRowMapper, doctorId, Date.valueOf(date));
    }

    public List<Appointment> findDoctorTodayAppointments(Long doctorId, LocalDate today) {
        String sql = BASE_APPOINTMENT_QUERY + " WHERE a.doctor_id = ? AND a.appointment_date = ? ORDER BY a.start_time ASC";
        return jdbcTemplate.query(sql, appointmentRowMapper, doctorId, Date.valueOf(today));
    }

    public List<Appointment> findAllWithFilters(Long doctorId, Long patientId, String status, LocalDate fromDate, LocalDate toDate) {
        StringBuilder sql = new StringBuilder(BASE_APPOINTMENT_QUERY + " WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (doctorId != null && doctorId > 0) {
            sql.append(" AND a.doctor_id = ? ");
            params.add(doctorId);
        }
        if (patientId != null && patientId > 0) {
            sql.append(" AND a.patient_id = ? ");
            params.add(patientId);
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND a.appointment_status = ? ");
            params.add(status.trim().toUpperCase());
        }
        if (fromDate != null) {
            sql.append(" AND a.appointment_date >= ? ");
            params.add(Date.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append(" AND a.appointment_date <= ? ");
            params.add(Date.valueOf(toDate));
        }

        sql.append(" ORDER BY a.appointment_date DESC, a.start_time DESC");
        return jdbcTemplate.query(sql.toString(), appointmentRowMapper, params.toArray());
    }

    /**
     * Checks if any active (non-cancelled) appointment conflicts with the requested slot for a doctor.
     * Uses pessimistic locking query to prevent concurrent double-booking.
     */
    public boolean hasConflictingAppointmentWithLock(Long doctorId, LocalDate date, LocalTime startTime, LocalTime endTime, Long excludeAppointmentId) {
        StringBuilder sql = new StringBuilder("""
            SELECT appointment_id FROM appointments 
            WHERE doctor_id = ? 
              AND appointment_date = ? 
              AND appointment_status NOT IN ('CANCELLED')
              AND ((start_time < ? AND end_time > ?) OR (start_time >= ? AND start_time < ?))
        """);

        List<Object> params = new ArrayList<>();
        params.add(doctorId);
        params.add(Date.valueOf(date));
        params.add(Time.valueOf(endTime));
        params.add(Time.valueOf(startTime));
        params.add(Time.valueOf(startTime));
        params.add(Time.valueOf(endTime));

        if (excludeAppointmentId != null) {
            sql.append(" AND appointment_id <> ? ");
            params.add(excludeAppointmentId);
        }

        sql.append(" FOR UPDATE LIMIT 1");

        List<Long> ids = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong(1), params.toArray());
        return !ids.isEmpty();
    }

    public Long save(Appointment a) {
        String sql = """
            INSERT INTO appointments (appointment_reference, patient_id, doctor_id, appointment_date, 
                                     start_time, end_time, appointment_status, consultation_fee_snapshot, 
                                     appointment_type, patient_notes, doctor_instructions)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[] { "appointment_id" });
            ps.setString(1, a.getAppointmentReference());
            ps.setLong(2, a.getPatientId());
            ps.setLong(3, a.getDoctorId());
            ps.setDate(4, Date.valueOf(a.getAppointmentDate()));
            ps.setTime(5, Time.valueOf(a.getStartTime()));
            ps.setTime(6, Time.valueOf(a.getEndTime()));
            ps.setString(7, a.getAppointmentStatus() != null ? a.getAppointmentStatus() : "CONFIRMED");
            ps.setBigDecimal(8, a.getConsultationFeeSnapshot());
            ps.setString(9, a.getAppointmentType() != null ? a.getAppointmentType() : "IN_PERSON");
            ps.setString(10, a.getPatientNotes());
            ps.setString(11, a.getDoctorInstructions());
            return ps;
        }, keyHolder);
        return KeyUtils.getGeneratedId(keyHolder, "appointment_id");
    }

    public void updateStatus(Long appointmentId, String status, String reason, String instructions) {
        StringBuilder sql = new StringBuilder("UPDATE appointments SET appointment_status = ? ");
        List<Object> params = new ArrayList<>();
        params.add(status);

        if (reason != null) {
            sql.append(", cancellation_reason = ? ");
            params.add(reason);
        }
        if (instructions != null) {
            sql.append(", doctor_instructions = ? ");
            params.add(instructions);
        }

        sql.append(" WHERE appointment_id = ?");
        params.add(appointmentId);

        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    public void reschedule(Long appointmentId, LocalDate newDate, LocalTime newStartTime, LocalTime newEndTime) {
        String sql = "UPDATE appointments SET appointment_date = ?, start_time = ?, end_time = ? WHERE appointment_id = ?";
        jdbcTemplate.update(sql, Date.valueOf(newDate), Time.valueOf(newStartTime), Time.valueOf(newEndTime), appointmentId);
    }

    public int countTodayAppointments(LocalDate today) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE appointment_date = ? AND appointment_status NOT IN ('CANCELLED')";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, Date.valueOf(today));
        return count != null ? count : 0;
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE appointment_status = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, status);
        return count != null ? count : 0;
    }

    public int countDoctorTodayAppointments(Long doctorId, LocalDate today) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND appointment_date = ? AND appointment_status NOT IN ('CANCELLED')";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, doctorId, Date.valueOf(today));
        return count != null ? count : 0;
    }

    public int countDoctorUpcomingAppointments(Long doctorId, LocalDate today) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND appointment_date >= ? AND appointment_status IN ('CONFIRMED', 'PENDING_CONFIRMATION')";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, doctorId, Date.valueOf(today));
        return count != null ? count : 0;
    }

    public int countDoctorCompletedToday(Long doctorId, LocalDate today) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND appointment_date = ? AND appointment_status = 'COMPLETED'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, doctorId, Date.valueOf(today));
        return count != null ? count : 0;
    }

    public int countDoctorPendingConfirmations(Long doctorId) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND appointment_status = 'PENDING_CONFIRMATION'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, doctorId);
        return count != null ? count : 0;
    }

    public int countDoctorUniquePatients(Long doctorId) {
        String sql = "SELECT COUNT(DISTINCT patient_id) FROM appointments WHERE doctor_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, doctorId);
        return count != null ? count : 0;
    }

    public BigDecimal sumDoctorMonthlyEarnings(Long doctorId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        String sql = """
            SELECT COALESCE(SUM(consultation_fee_snapshot), 0) 
            FROM appointments 
            WHERE doctor_id = ? 
              AND appointment_status = 'COMPLETED'
              AND appointment_date >= ? AND appointment_date < ?
        """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, doctorId, Date.valueOf(start), Date.valueOf(end));
    }
}
