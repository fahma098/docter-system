package com.healthcare.app.repository;

import com.healthcare.app.entity.AppointmentHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AppointmentHistoryRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<AppointmentHistory> rowMapper = (rs, rowNum) -> {
        AppointmentHistory h = new AppointmentHistory();
        h.setHistoryId(rs.getLong("history_id"));
        h.setAppointmentId(rs.getLong("appointment_id"));
        long actorId = rs.getLong("actor_user_id");
        if (!rs.wasNull()) {
            h.setActorUserId(actorId);
        }
        h.setAction(rs.getString("action"));
        h.setOldStatus(rs.getString("old_status"));
        h.setNewStatus(rs.getString("new_status"));
        h.setReason(rs.getString("reason"));
        if (rs.getTimestamp("created_at") != null) {
            h.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        try {
            h.setActorName(rs.getString("actor_name"));
        } catch (Exception ignored) {}
        return h;
    };

    public void logHistory(Long appointmentId, Long actorUserId, String action, String oldStatus, String newStatus, String reason) {
        String sql = "INSERT INTO appointment_history (appointment_id, actor_user_id, action, old_status, new_status, reason) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, appointmentId, actorUserId, action, oldStatus, newStatus, reason);
    }

    public List<AppointmentHistory> findByAppointmentId(Long appointmentId) {
        String sql = """
            SELECT h.*, u.full_name AS actor_name
            FROM appointment_history h
            LEFT JOIN users u ON h.actor_user_id = u.user_id
            WHERE h.appointment_id = ?
            ORDER BY h.created_at ASC
        """;
        return jdbcTemplate.query(sql, rowMapper, appointmentId);
    }
}
