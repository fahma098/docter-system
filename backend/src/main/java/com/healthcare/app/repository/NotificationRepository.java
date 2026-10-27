package com.healthcare.app.repository;

import com.healthcare.app.entity.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class NotificationRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Notification> rowMapper = (rs, rowNum) -> {
        Notification n = new Notification();
        n.setNotificationId(rs.getLong("notification_id"));
        n.setUserId(rs.getLong("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setReadStatus(rs.getBoolean("read_status"));
        if (rs.getTimestamp("created_at") != null) {
            n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return n;
    };

    public List<Notification> findByUserId(Long userId) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }

    public int countUnreadByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read_status = FALSE";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count : 0;
    }

    public Long save(Notification n) {
        String sql = "INSERT INTO notifications (user_id, title, message, type, read_status) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, n.getUserId());
            ps.setString(2, n.getTitle());
            ps.setString(3, n.getMessage());
            ps.setString(4, n.getType() != null ? n.getType() : "INFO");
            ps.setBoolean(5, n.isReadStatus());
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public void markAllAsRead(Long userId) {
        String sql = "UPDATE notifications SET read_status = TRUE WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }

    public void markAsRead(Long notificationId, Long userId) {
        String sql = "UPDATE notifications SET read_status = TRUE WHERE notification_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, notificationId, userId);
    }
}
