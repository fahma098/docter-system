package com.healthcare.app.controller;

import com.healthcare.app.dto.ApiResponse;
import com.healthcare.app.dto.AuthResponse;
import com.healthcare.app.dto.LoginRequest;
import com.healthcare.app.dto.RegisterPatientRequest;
import com.healthcare.app.security.SecurityContextHelper;
import com.healthcare.app.security.SessionTokenManager;
import com.healthcare.app.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse resp = authService.login(req);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", resp));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterPatientRequest req) {
        AuthResponse resp = authService.registerPatient(req);
        return ResponseEntity.ok(ApiResponse.ok("Patient registration successful", resp));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7).trim());
        }
        return ResponseEntity.ok(ApiResponse.okMessage("Successfully logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SessionTokenManager.UserSession>> getMe() {
        SessionTokenManager.UserSession session = SecurityContextHelper.getCurrentSession();
        return ResponseEntity.ok(ApiResponse.ok(session));
    }
}
