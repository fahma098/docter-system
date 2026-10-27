package com.healthcare.app.repository;

import com.healthcare.app.entity.Doctor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DoctorRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Doctor> rowMapper = (rs, rowNum) -> {
        Doctor d = new Doctor();
        d.setDoctorId(rs.getLong("doctor_id"));
        d.setUserId(rs.getLong("user_id"));
        d.setSpecializationId(rs.getLong("specialization_id"));
        d.setLicenseNumber(rs.getString("license_number"));
        d.setQualifications(rs.getString("qualifications"));
        d.setExperienceYears(rs.getInt("experience_years"));
        d.setConsultationFee(rs.getBigDecimal("consultation_fee"));
        d.setDefaultAppointmentDuration(rs.getInt("default_appointment_duration"));
        d.setProfessionalBio(rs.getString("professional_bio"));
        d.setProfileImageUrl(rs.getString("profile_image_url"));
        d.setApprovalStatus(rs.getString("approval_status"));
        if (rs.getTimestamp("created_at") != null) {
            d.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("updated_at") != null) {
            d.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }

        try {
            d.setFullName(rs.getString("full_name"));
            d.setEmail(rs.getString("email"));
            d.setPhone(rs.getString("phone"));
            d.setSpecializationName(rs.getString("specialization_name"));
        } catch (Exception ignored) {}

        return d;
    };

    private static final String BASE_DOCTOR_SELECT = """
        SELECT d.*, u.full_name, u.email, u.phone, s.specialization_name
        FROM doctors d
        JOIN users u ON d.user_id = u.user_id
        JOIN specializations s ON d.specialization_id = s.specialization_id
    """;

    public Optional<Doctor> findById(Long doctorId) {
        String sql = BASE_DOCTOR_SELECT + " WHERE d.doctor_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, doctorId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Doctor> findByUserId(Long userId) {
        String sql = BASE_DOCTOR_SELECT + " WHERE d.user_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, userId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Doctor> searchDoctors(Long specializationId, String query, Boolean approvedOnly) {
        StringBuilder sql = new StringBuilder(BASE_DOCTOR_SELECT + " WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (Boolean.TRUE.equals(approvedOnly)) {
            sql.append(" AND d.approval_status = 'APPROVED' AND u.account_status = 'ACTIVE' ");
        }
        if (specializationId != null && specializationId > 0) {
            sql.append(" AND d.specialization_id = ? ");
            params.add(specializationId);
        }
        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (LOWER(u.full_name) LIKE ? OR LOWER(s.specialization_name) LIKE ? OR LOWER(d.qualifications) LIKE ?) ");
            String wild = "%" + query.trim().toLowerCase() + "%";
            params.add(wild);
            params.add(wild);
            params.add(wild);
        }

        sql.append(" ORDER BY u.full_name ASC");
        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    public Long save(Doctor doctor) {
        String sql = """
            INSERT INTO doctors (user_id, specialization_id, license_number, qualifications, 
                                experience_years, consultation_fee, default_appointment_duration, 
                                professional_bio, profile_image_url, approval_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, doctor.getUserId());
            ps.setLong(2, doctor.getSpecializationId());
            ps.setString(3, doctor.getLicenseNumber());
            ps.setString(4, doctor.getQualifications());
            ps.setInt(5, doctor.getExperienceYears());
            ps.setBigDecimal(6, doctor.getConsultationFee());
            ps.setInt(7, doctor.getDefaultAppointmentDuration());
            ps.setString(8, doctor.getProfessionalBio());
            ps.setString(9, doctor.getProfileImageUrl());
            ps.setString(10, doctor.getApprovalStatus() != null ? doctor.getApprovalStatus() : "APPROVED");
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public void update(Doctor d) {
        String sql = """
            UPDATE doctors SET specialization_id = ?, qualifications = ?, experience_years = ?, 
                               consultation_fee = ?, default_appointment_duration = ?, 
                               professional_bio = ?, profile_image_url = ?
            WHERE doctor_id = ?
        """;
        jdbcTemplate.update(sql,
                d.getSpecializationId(),
                d.getQualifications(),
                d.getExperienceYears(),
                d.getConsultationFee(),
                d.getDefaultAppointmentDuration(),
                d.getProfessionalBio(),
                d.getProfileImageUrl(),
                d.getDoctorId()
        );
    }

    public void updateApprovalStatus(Long doctorId, String status) {
        String sql = "UPDATE doctors SET approval_status = ? WHERE doctor_id = ?";
        jdbcTemplate.update(sql, status, doctorId);
    }

    public int countTotalDoctors() {
        String sql = "SELECT COUNT(*) FROM doctors";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }
}
