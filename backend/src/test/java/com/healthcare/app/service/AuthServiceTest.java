package com.healthcare.app.service;

import com.healthcare.app.dto.AuthResponse;
import com.healthcare.app.dto.LoginRequest;
import com.healthcare.app.dto.RegisterPatientRequest;
import com.healthcare.app.entity.User;
import com.healthcare.app.exception.BusinessValidationException;
import com.healthcare.app.exception.UnauthorizedException;
import com.healthcare.app.repository.*;
import com.healthcare.app.security.SessionTokenManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SessionTokenManager sessionTokenManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should successfully authenticate valid patient credentials and return session token")
    void shouldLoginSuccessfully() {
        User user = new User(1L, "John Miller", "patient.john@healthcare.com", "+1-800-555-0121", "$2a$10$hashedpassword", "PATIENT", "ACTIVE");

        when(userRepository.findByEmail("patient.john@healthcare.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", user.getPasswordHash())).thenReturn(true);

        SessionTokenManager.UserSession session = new SessionTokenManager.UserSession(
                "mock-token-xyz-123", 1L, user.getEmail(), user.getFullName(), "PATIENT", 5L, null, Instant.now().plusSeconds(3600));

        when(sessionTokenManager.createSession(eq(1L), anyString(), anyString(), eq("PATIENT"), any(), isNull()))
                .thenReturn(session);

        LoginRequest req = new LoginRequest("patient.john@healthcare.com", "Password123!");
        AuthResponse resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("mock-token-xyz-123", resp.getToken());
        assertEquals("PATIENT", resp.getRole());
        assertEquals("John Miller", resp.getFullName());
    }

    @Test
    @DisplayName("Should reject login with invalid password")
    void shouldRejectInvalidPassword() {
        User user = new User(1L, "John Miller", "patient.john@healthcare.com", "+1-800-555-0121", "$2a$10$hashedpassword", "PATIENT", "ACTIVE");

        when(userRepository.findByEmail("patient.john@healthcare.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", user.getPasswordHash())).thenReturn(false);

        LoginRequest req = new LoginRequest("patient.john@healthcare.com", "WrongPassword");

        assertThrows(UnauthorizedException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("Should reject registration with mismatched confirm password")
    void shouldRejectMismatchedPasswordRegistration() {
        RegisterPatientRequest req = new RegisterPatientRequest();
        req.setFullName("Emma Watson");
        req.setEmail("emma@example.com");
        req.setPhone("+1-800-555-0122");
        req.setPassword("Password123!");
        req.setConfirmPassword("DifferentPassword456!");

        assertThrows(BusinessValidationException.class, () -> authService.registerPatient(req));
    }
}
