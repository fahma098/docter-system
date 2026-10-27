package com.healthcare.app.repository;

import com.healthcare.app.entity.SystemSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class SystemSettingRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<SystemSetting> rowMapper = (rs, rowNum) -> new SystemSetting(
            rs.getString("setting_key"),
            rs.getString("setting_value"),
            rs.getString("description")
    );

    public Optional<String> getValue(String key) {
        String sql = "SELECT setting_value FROM system_settings WHERE setting_key = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, String.class, key));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public String getValueOrDefault(String key, String defaultValue) {
        return getValue(key).orElse(defaultValue);
    }

    public void setSetting(String key, String value, String description) {
        String checkSql = "SELECT COUNT(*) FROM system_settings WHERE setting_key = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, key);
        if (count != null && count > 0) {
            String updateSql = "UPDATE system_settings SET setting_value = ?, description = ? WHERE setting_key = ?";
            jdbcTemplate.update(updateSql, value, description, key);
        } else {
            String insertSql = "INSERT INTO system_settings (setting_key, setting_value, description) VALUES (?, ?, ?)";
            jdbcTemplate.update(insertSql, key, value, description);
        }
    }

    public Map<String, String> getAllSettings() {
        String sql = "SELECT * FROM system_settings";
        List<SystemSetting> list = jdbcTemplate.query(sql, rowMapper);
        Map<String, String> map = new HashMap<>();
        for (SystemSetting s : list) {
            map.put(s.getSettingKey(), s.getSettingValue());
        }
        return map;
    }
}
