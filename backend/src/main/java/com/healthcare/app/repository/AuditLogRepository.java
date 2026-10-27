package com.healthcare.app.repository;

import com.healthcare.app.entity.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuditLogRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<AuditLog> rowMapper = (rs, rowNum) -> {
        AuditLog a = new AuditLog();
        a.setAuditId(rs.getLong("audit_id"));
        long actorId = rs.getLong("actor_user_id");
        if (!rs.wasNull()) {
            a.setActorUserId(actorId);
        }
        a.setAction(rs.getString("action"));
        a.setEntityType(rs.getString("entity_type"));
        a.setEntityId(rs.getString("entity_id"));
        a.setDetails(rs.getString("details"));
        if (rs.getTimestamp("created_at") != null) {
            a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        try {
            a.setActorName(rs.getString("actor_name"));
            a.setActorRole(rs.getString("actor_role"));
        } catch (Exception ignored) {}
        return a;
    };

    public void log(Long actorUserId, String action, String entityType, String entityId, String details) {
        String sql = "INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql);
            if (actorUserId != null) {
                ps.setLong(1, actorUserId);
            } else {
                ps.setNull(1, java.sql.Types.BIGINT);
            }
            ps.setString(2, action);
            ps.setString(3, entityType);
            ps.setString(4, entityId);
            ps.setString(5, details);
            return ps;
        });
    }

    public List<AuditLog> findRecent(int limit) {
        String sql = """
            SELECT a.*, u.full_name AS actor_name, u.role AS actor_role
            FROM audit_logs a
            LEFT JOIN users u ON a.actor_user_id = u.user_id
            ORDER BY a.created_at DESC
            LIMIT ?
        """;
        return jdbcTemplate.query(sql, rowMapper, limit);
    }
}
