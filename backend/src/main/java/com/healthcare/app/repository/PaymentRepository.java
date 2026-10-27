package com.healthcare.app.repository;

import com.healthcare.app.entity.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class PaymentRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Payment> rowMapper = (rs, rowNum) -> {
        Payment p = new Payment();
        p.setPaymentId(rs.getLong("payment_id"));
        p.setAppointmentId(rs.getLong("appointment_id"));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setPaymentMethod(rs.getString("payment_method"));
        p.setPaymentStatus(rs.getString("payment_status"));
        p.setTransactionReference(rs.getString("transaction_reference"));
        if (rs.getTimestamp("recorded_at") != null) {
            p.setRecordedAt(rs.getTimestamp("recorded_at").toLocalDateTime());
        }
        return p;
    };

    public Optional<Payment> findByAppointmentId(Long appointmentId) {
        String sql = "SELECT * FROM payments WHERE appointment_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, appointmentId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Long save(Payment payment) {
        String sql = "INSERT INTO payments (appointment_id, amount, payment_method, payment_status, transaction_reference) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, payment.getAppointmentId());
            ps.setBigDecimal(2, payment.getAmount());
            ps.setString(3, payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "CREDIT_CARD");
            ps.setString(4, payment.getPaymentStatus() != null ? payment.getPaymentStatus() : "PAID");
            ps.setString(5, payment.getTransactionReference());
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public void updateStatus(Long appointmentId, String status) {
        String sql = "UPDATE payments SET payment_status = ? WHERE appointment_id = ?";
        jdbcTemplate.update(sql, status, appointmentId);
    }

    public BigDecimal sumPaidRevenue() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE payment_status = 'PAID'";
        BigDecimal total = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        return total != null ? total : BigDecimal.ZERO;
    }
}
