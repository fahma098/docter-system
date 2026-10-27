package com.healthcare.app.repository;

import com.healthcare.app.entity.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class PatientRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Patient> rowMapper = (rs, rowNum) -> {
        Patient p = new Patient();
        p.setPatientId(rs.getLong("patient_id"));
        p.setUserId(rs.getLong("user_id"));
        if (rs.getDate("date_of_birth") != null) {
            p.setDateOfBirth(rs.getDate("date_of_birth").toLocalDate());
        }
        p.setGender(rs.getString("gender"));
        p.setBloodGroup(rs.getString("blood_group"));
        p.setAddress(rs.getString("address"));
        p.setEmergencyContact(rs.getString("emergency_contact"));
        if (rs.getTimestamp("created_at") != null) {
            p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("updated_at") != null) {
            p.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }

        // Try getting joined user columns if present
        try {
            p.setFullName(rs.getString("full_name"));
            p.setEmail(rs.getString("email"));
            p.setPhone(rs.getString("phone"));
        } catch (Exception ignored) {}

        return p;
    };

    public Optional<Patient> findById(Long patientId) {
        String sql = """
            SELECT p.*, u.full_name, u.email, u.phone 
            FROM patients p
            JOIN users u ON p.user_id = u.user_id
            WHERE p.patient_id = ?
        """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, patientId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Patient> findByUserId(Long userId) {
        String sql = """
            SELECT p.*, u.full_name, u.email, u.phone 
            FROM patients p
            JOIN users u ON p.user_id = u.user_id
            WHERE p.user_id = ?
        """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, userId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Long save(Patient patient) {
        String sql = "INSERT INTO patients (user_id, date_of_birth, gender, blood_group, address, emergency_contact) VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[] { "patient_id" });
            ps.setLong(1, patient.getUserId());
            if (patient.getDateOfBirth() != null) {
                ps.setDate(2, Date.valueOf(patient.getDateOfBirth()));
            } else {
                ps.setNull(2, java.sql.Types.DATE);
            }
            ps.setString(3, patient.getGender());
            ps.setString(4, patient.getBloodGroup());
            ps.setString(5, patient.getAddress());
            ps.setString(6, patient.getEmergencyContact());
            return ps;
        }, keyHolder);
        return KeyUtils.getGeneratedId(keyHolder, "patient_id");
    }

    public void update(Patient patient) {
        String sql = "UPDATE patients SET date_of_birth = ?, gender = ?, blood_group = ?, address = ?, emergency_contact = ? WHERE patient_id = ?";
        Date dob = patient.getDateOfBirth() != null ? Date.valueOf(patient.getDateOfBirth()) : null;
        jdbcTemplate.update(sql,
                dob,
                patient.getGender(),
                patient.getBloodGroup(),
                patient.getAddress(),
                patient.getEmergencyContact(),
                patient.getPatientId()
        );
    }

    public List<Patient> findAll() {
        String sql = """
            SELECT p.*, u.full_name, u.email, u.phone 
            FROM patients p
            JOIN users u ON p.user_id = u.user_id
            ORDER BY p.created_at DESC
        """;
        return jdbcTemplate.query(sql, rowMapper);
    }

    public int countTotalPatients() {
        String sql = "SELECT COUNT(*) FROM patients";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }
}
