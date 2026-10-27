package com.healthcare.app.repository;

import org.springframework.jdbc.support.KeyHolder;

import java.util.Map;

/**
 * Universal utility for safely extracting generated primary keys across
 * MySQL, PostgreSQL, and H2 database drivers.
 *
 * Prevents Spring's InvalidDataAccessApiUsageException caused by PostgreSQL JDBC
 * returning all inserted columns as generated keys when keyHolder.getKey() is called.
 */
public class KeyUtils {

    private KeyUtils() {
        // Utility class
    }

    /**
     * Safely retrieves the numeric primary key from a KeyHolder.
     *
     * @param keyHolder     Spring KeyHolder populated by JdbcTemplate update
     * @param keyColumnName Expected primary key column name (e.g. "user_id")
     * @return Generated Long identifier, or null if not available
     */
    public static Long getGeneratedId(KeyHolder keyHolder, String keyColumnName) {
        if (keyHolder == null) {
            return null;
        }

        try {
            Map<String, Object> keys = keyHolder.getKeys();
            if (keys != null && !keys.isEmpty()) {
                // 1. Check exact key name match, lower-case, and upper-case
                Object val = keys.get(keyColumnName);
                if (val == null && keyColumnName != null) {
                    val = keys.get(keyColumnName.toLowerCase());
                }
                if (val == null && keyColumnName != null) {
                    val = keys.get(keyColumnName.toUpperCase());
                }
                if (val instanceof Number num) {
                    return num.longValue();
                }

                // 2. Check for first numeric value in the keys map (e.g. MySQL GENERATED_KEY)
                for (Object v : keys.values()) {
                    if (v instanceof Number num) {
                        return num.longValue();
                    }
                }
            }
        } catch (Exception ignored) {
            // If getKeys() threw or failed, fallback to getKey()
        }

        try {
            Number k = keyHolder.getKey();
            return k != null ? k.longValue() : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
