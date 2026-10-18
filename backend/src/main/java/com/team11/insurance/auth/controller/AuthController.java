package com.team11.insurance.auth.controller;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.dto.*;
import com.team11.insurance.auth.entity.User;
import com.team11.insurance.auth.filter.JwtAuthenticationFilter;
import com.team11.insurance.auth.repository.UserRepository;
import com.team11.insurance.auth.service.AuthenticationService;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for authentication and session lifecycle operations (INS-F-001, INS-SR-002, INS-SR-005).
 * <p>
 * Base path: {@code /auth} (serviced under servlet context-path {@code /api/v1}).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationService authenticationService, UserRepository userRepository) {
        this.authenticationService = authenticationService;
        this.userRepository = userRepository;
    }

    /**
     * Public user registration endpoint (INS-F-001, API Contract §2.1).
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Public user login endpoint (INS-F-001, API Contract §2.2).
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponse response = authenticationService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the currently authenticated user's identity profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(CurrentUser currentUser) {
        User user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getUserId().toString()));

        UserProfileResponse profile = new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().getValue(),
                user.getInsurerId()
        );
        return ResponseEntity.ok(profile);
    }

    /**
     * Invalidation of current session on logout.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(JwtAuthenticationFilter.BEARER_PREFIX)) {
            String token = authHeader.substring(JwtAuthenticationFilter.BEARER_PREFIX.length()).trim();
            authenticationService.logout(token);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }
}
