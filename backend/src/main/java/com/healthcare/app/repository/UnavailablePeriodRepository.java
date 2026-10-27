package com.healthcare.app.repository;

import com.healthcare.app.entity.DoctorUnavailablePeriod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class UnavailablePeriodRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<DoctorUnavailablePeriod> rowMapper = (rs, rowNum) -> {
        DoctorUnavailablePeriod p = new DoctorUnavailablePeriod();
        p.setUnavailableId(rs.getLong("unavailable_id"));
        p.setDoctorId(rs.getLong("doctor_id"));
        if (rs.getTimestamp("start_datetime") != null) {
            p.setStartDatetime(rs.getTimestamp("start_datetime").toLocalDateTime());
        }
        if (rs.getTimestamp("end_datetime") != null) {
            p.setEndDatetime(rs.getTimestamp("end_datetime").toLocalDateTime());
        }
        p.setReason(rs.getString("reason"));
        p.setApprovalStatus(rs.getString("approval_status"));
        if (rs.getTimestamp("created_at") != null) {
            p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return p;
    };

    public List<DoctorUnavailablePeriod> findByDoctorId(Long doctorId) {
        String sql = "SELECT * FROM doctor_unavailable_periods WHERE doctor_id = ? ORDER BY start_datetime DESC";
        return jdbcTemplate.query(sql, rowMapper, doctorId);
    }

    public List<DoctorUnavailablePeriod> findApprovedPeriodsInRange(Long doctorId, LocalDateTime from, LocalDateTime to) {
        String sql = """
            SELECT * FROM doctor_unavailable_periods 
            WHERE doctor_id = ? AND approval_status = 'APPROVED'
              AND start_datetime <= ? AND end_datetime >= ?
        """;
        return jdbcTemplate.query(sql, rowMapper, doctorId, Timestamp.valueOf(to), Timestamp.valueOf(from));
    }

    public boolean isDoctorUnavailable(Long doctorId, LocalDateTime start, LocalDateTime end) {
        String sql = """
            SELECT COUNT(*) FROM doctor_unavailable_periods 
            WHERE doctor_id = ? AND approval_status = 'APPROVED'
              AND ((start_datetime < ? AND end_datetime > ?) OR (start_datetime >= ? AND start_datetime < ?))
        """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                doctorId,
                Timestamp.valueOf(end), Timestamp.valueOf(start),
                Timestamp.valueOf(start), Timestamp.valueOf(end));
        return count != null && count > 0;
    }

    public Long save(DoctorUnavailablePeriod p) {
        String sql = "INSERT INTO doctor_unavailable_periods (doctor_id, start_datetime, end_datetime, reason, approval_status) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[] { "unavailable_id" });
            ps.setLong(1, p.getDoctorId());
            ps.setTimestamp(2, Timestamp.valueOf(p.getStartDatetime()));
            ps.setTimestamp(3, Timestamp.valueOf(p.getEndDatetime()));
            ps.setString(4, p.getReason());
            ps.setString(5, p.getApprovalStatus() != null ? p.getApprovalStatus() : "APPROVED");
            return ps;
        }, keyHolder);
        return KeyUtils.getGeneratedId(keyHolder, "unavailable_id");
    }

    public void updateApprovalStatus(Long unavailableId, String status) {
        String sql = "UPDATE doctor_unavailable_periods SET approval_status = ? WHERE unavailable_id = ?";
        jdbcTemplate.update(sql, status, unavailableId);
    }
}
