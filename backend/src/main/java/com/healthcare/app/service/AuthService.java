package com.healthcare.app.service;

import com.healthcare.app.dto.AuthResponse;
import com.healthcare.app.dto.LoginRequest;
import com.healthcare.app.dto.RegisterPatientRequest;
import com.healthcare.app.entity.Doctor;
import com.healthcare.app.entity.Notification;
import com.healthcare.app.entity.Patient;
import com.healthcare.app.entity.User;
import com.healthcare.app.exception.BusinessValidationException;
import com.healthcare.app.exception.UnauthorizedException;
import com.healthcare.app.repository.*;
import com.healthcare.app.security.SessionTokenManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SessionTokenManager sessionTokenManager;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
            throw new UnauthorizedException("Your account is currently " + user.getAccountStatus().toLowerCase() + ". Please contact administration.");
        }

        Long patientId = null;
        Long doctorId = null;

        if ("PATIENT".equals(user.getRole())) {
            Optional<Patient> patientOpt = patientRepository.findByUserId(user.getUserId());
            patientId = patientOpt.map(Patient::getPatientId).orElse(null);
        } else if ("DOCTOR".equals(user.getRole())) {
            Optional<Doctor> doctorOpt = doctorRepository.findByUserId(user.getUserId());
            doctorId = doctorOpt.map(Doctor::getDoctorId).orElse(null);
        }

        SessionTokenManager.UserSession session = sessionTokenManager.createSession(
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                patientId,
                doctorId
        );

        auditLogRepository.log(user.getUserId(), "LOGIN", "USER", String.valueOf(user.getUserId()), "Successful authentication");

        return new AuthResponse(
                session.getToken(),
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                patientId,
                doctorId
        );
    }

    @Transactional
    public AuthResponse registerPatient(RegisterPatientRequest req) {
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessValidationException("Passwords do not match");
        }

        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessValidationException("An account with email " + email + " already exists");
        }

        String hashed = passwordEncoder.encode(req.getPassword());
        User user = new User(null, req.getFullName().trim(), email, req.getPhone().trim(), hashed, "PATIENT", "ACTIVE");
        Long userId = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUserId(userId);
        patient.setDateOfBirth(req.getDateOfBirth());
        patient.setGender(req.getGender());
        patient.setBloodGroup(req.getBloodGroup());
        patient.setAddress(req.getAddress());
        patient.setEmergencyContact(req.getEmergencyContact());
        Long patientId = patientRepository.save(patient);

        // Welcome Notification
        Notification welcomeNote = new Notification();
        welcomeNote.setUserId(userId);
        welcomeNote.setTitle("Welcome to Aura Healthcare");
        welcomeNote.setMessage("Welcome " + user.getFullName() + "! You can now search top specialist doctors, view available slots, and book your appointments easily.");
        welcomeNote.setType("SYSTEM");
        welcomeNote.setReadStatus(false);
        notificationRepository.save(welcomeNote);

        auditLogRepository.log(userId, "REGISTER_PATIENT", "PATIENT", String.valueOf(patientId), "New patient registered: " + user.getEmail());

        SessionTokenManager.UserSession session = sessionTokenManager.createSession(
                userId,
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                patientId,
                null
        );

        return new AuthResponse(
                session.getToken(),
                userId,
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                patientId,
                null
        );
    }

    public void logout(String token) {
        sessionTokenManager.invalidateSession(token);
    }
}
