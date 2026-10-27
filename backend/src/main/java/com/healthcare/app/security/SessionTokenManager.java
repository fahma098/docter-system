package com.healthcare.app.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionTokenManager {

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    public static class UserSession {
        private final String token;
        private final Long userId;
        private final String email;
        private final String fullName;
        private final String role;
        private final Long patientId;
        private final Long doctorId;
        private final Instant expiresAt;

        public UserSession(String token, Long userId, String email, String fullName, String role, Long patientId, Long doctorId, Instant expiresAt) {
            this.token = token;
            this.userId = userId;
            this.email = email;
            this.fullName = fullName;
            this.role = role;
            this.patientId = patientId;
            this.doctorId = doctorId;
            this.expiresAt = expiresAt;
        }

        public String getToken() { return token; }
        public Long getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public String getRole() { return role; }
        public Long getPatientId() { return patientId; }
        public Long getDoctorId() { return doctorId; }
        public Instant getExpiresAt() { return expiresAt; }
        public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    }

    public UserSession createSession(Long userId, String email, String fullName, String role, Long patientId, Long doctorId) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // 7 days session validity
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        UserSession session = new UserSession(token, userId, email, fullName, role, patientId, doctorId, expiresAt);
        sessions.put(token, session);
        return session;
    }

    public Optional<UserSession> getSession(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        UserSession session = sessions.get(token);
        if (session != null) {
            if (session.isExpired()) {
                sessions.remove(token);
                return Optional.empty();
            }
            return Optional.of(session);
        }
        return Optional.empty();
    }

    public void invalidateSession(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
