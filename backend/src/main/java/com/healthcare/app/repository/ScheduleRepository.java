package com.healthcare.app.repository;

import com.healthcare.app.entity.DoctorSchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Time;
import java.util.List;
import java.util.Optional;

@Repository
public class ScheduleRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<DoctorSchedule> rowMapper = (rs, rowNum) -> {
        DoctorSchedule s = new DoctorSchedule();
        s.setScheduleId(rs.getLong("schedule_id"));
        s.setDoctorId(rs.getLong("doctor_id"));
        s.setWorkingDay(rs.getString("working_day"));
        if (rs.getTime("start_time") != null) {
            s.setStartTime(rs.getTime("start_time").toLocalTime());
        }
        if (rs.getTime("end_time") != null) {
            s.setEndTime(rs.getTime("end_time").toLocalTime());
        }
        if (rs.getTime("break_start_time") != null) {
            s.setBreakStartTime(rs.getTime("break_start_time").toLocalTime());
        }
        if (rs.getTime("break_end_time") != null) {
            s.setBreakEndTime(rs.getTime("break_end_time").toLocalTime());
        }
        s.setSlotDurationMinutes(rs.getInt("slot_duration_minutes"));
        s.setActiveStatus(rs.getBoolean("active_status"));
        if (rs.getTimestamp("created_at") != null) {
            s.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return s;
    };

    public List<DoctorSchedule> findByDoctorId(Long doctorId) {
        String sql = """
            SELECT * FROM doctor_schedules 
            WHERE doctor_id = ? 
            ORDER BY CASE working_day 
                WHEN 'MONDAY' THEN 1 
                WHEN 'TUESDAY' THEN 2 
                WHEN 'WEDNESDAY' THEN 3 
                WHEN 'THURSDAY' THEN 4 
                WHEN 'FRIDAY' THEN 5 
                WHEN 'SATURDAY' THEN 6 
                WHEN 'SUNDAY' THEN 7 
                ELSE 8 END
        """;
        return jdbcTemplate.query(sql, rowMapper, doctorId);
    }

    public Optional<DoctorSchedule> findByDoctorAndDay(Long doctorId, String workingDay) {
        String sql = "SELECT * FROM doctor_schedules WHERE doctor_id = ? AND working_day = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, doctorId, workingDay.toUpperCase()));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Long save(DoctorSchedule schedule) {
        String sql = """
            INSERT INTO doctor_schedules (doctor_id, working_day, start_time, end_time, break_start_time, break_end_time, slot_duration_minutes, active_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[] { "schedule_id" });
            ps.setLong(1, schedule.getDoctorId());
            ps.setString(2, schedule.getWorkingDay().toUpperCase());
            ps.setTime(3, Time.valueOf(schedule.getStartTime()));
            ps.setTime(4, Time.valueOf(schedule.getEndTime()));
            if (schedule.getBreakStartTime() != null) {
                ps.setTime(5, Time.valueOf(schedule.getBreakStartTime()));
            } else {
                ps.setNull(5, java.sql.Types.TIME);
            }
            if (schedule.getBreakEndTime() != null) {
                ps.setTime(6, Time.valueOf(schedule.getBreakEndTime()));
            } else {
                ps.setNull(6, java.sql.Types.TIME);
            }
            ps.setInt(7, schedule.getSlotDurationMinutes() > 0 ? schedule.getSlotDurationMinutes() : 30);
            ps.setBoolean(8, schedule.isActiveStatus());
            return ps;
        }, keyHolder);
        return KeyUtils.getGeneratedId(keyHolder, "schedule_id");
    }

    public void update(DoctorSchedule s) {
        String sql = """
            UPDATE doctor_schedules 
            SET start_time = ?, end_time = ?, break_start_time = ?, break_end_time = ?, slot_duration_minutes = ?, active_status = ?
            WHERE schedule_id = ?
        """;
        jdbcTemplate.update(sql,
                Time.valueOf(s.getStartTime()),
                Time.valueOf(s.getEndTime()),
                s.getBreakStartTime() != null ? Time.valueOf(s.getBreakStartTime()) : null,
                s.getBreakEndTime() != null ? Time.valueOf(s.getBreakEndTime()) : null,
                s.getSlotDurationMinutes(),
                s.isActiveStatus(),
                s.getScheduleId()
        );
    }

    public void deleteByDoctorId(Long doctorId) {
        String sql = "DELETE FROM doctor_schedules WHERE doctor_id = ?";
        jdbcTemplate.update(sql, doctorId);
    }
}
