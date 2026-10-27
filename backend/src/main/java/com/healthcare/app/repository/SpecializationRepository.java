package com.healthcare.app.repository;

import com.healthcare.app.entity.Specialization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class SpecializationRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Specialization> rowMapper = (rs, rowNum) -> {
        Specialization s = new Specialization();
        s.setSpecializationId(rs.getLong("specialization_id"));
        s.setSpecializationName(rs.getString("specialization_name"));
        s.setDescription(rs.getString("description"));
        s.setIconName(rs.getString("icon_name"));
        s.setActiveStatus(rs.getBoolean("active_status"));
        if (rs.getTimestamp("created_at") != null) {
            s.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return s;
    };

    public List<Specialization> findAllActive() {
        String sql = "SELECT * FROM specializations WHERE active_status = TRUE ORDER BY specialization_name ASC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public List<Specialization> findAll() {
        String sql = "SELECT * FROM specializations ORDER BY specialization_name ASC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Specialization> findById(Long id) {
        String sql = "SELECT * FROM specializations WHERE specialization_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Long save(Specialization s) {
        String sql = "INSERT INTO specializations (specialization_name, description, icon_name, active_status) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[] { "specialization_id" });
            ps.setString(1, s.getSpecializationName());
            ps.setString(2, s.getDescription());
            ps.setString(3, s.getIconName() != null ? s.getIconName() : "stethoscope");
            ps.setBoolean(4, s.isActiveStatus());
            return ps;
        }, keyHolder);
        return KeyUtils.getGeneratedId(keyHolder, "specialization_id");
    }

    public void update(Specialization s) {
        String sql = "UPDATE specializations SET specialization_name = ?, description = ?, icon_name = ?, active_status = ? WHERE specialization_id = ?";
        jdbcTemplate.update(sql, s.getSpecializationName(), s.getDescription(), s.getIconName(), s.isActiveStatus(), s.getSpecializationId());
    }
}
