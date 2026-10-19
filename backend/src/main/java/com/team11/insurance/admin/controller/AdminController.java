package com.team11.insurance.admin.controller;

import com.team11.insurance.admin.dto.AdminCreateResponse;
import com.team11.insurance.admin.dto.CreateInsurerRequest;
import com.team11.insurance.admin.dto.CreateUserRequest;
import com.team11.insurance.admin.entity.Insurer;
import com.team11.insurance.admin.repository.InsurerRepository;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.RoleSecurity;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.auth.entity.User;
import com.team11.insurance.auth.repository.UserRepository;
import com.team11.insurance.auth.service.PasswordService;
import com.team11.insurance.common.exception.StateConflictException;
import com.team11.insurance.common.exception.ValidationException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for System Administration endpoints (SAD §4.3.6, API Contract §5).
 * <p>
 * Base path: {@code /admin} (serviced under servlet context-path {@code /api/v1}).
 * Restricted exclusively to {@link UserRole#SYSTEM_ADMIN}.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final InsurerRepository insurerRepository;
    private final PasswordService passwordService;

    public AdminController(UserRepository userRepository,
                           InsurerRepository insurerRepository,
                           PasswordService passwordService) {
        this.userRepository = userRepository;
        this.insurerRepository = insurerRepository;
        this.passwordService = passwordService;
    }

    /**
     * Creates a staff or system user account (SAD §4.3.6, API Contract §5.1).
     * Enforces the rule that staff members must be linked to an existing insurer.
     */
    @PostMapping("/users")
    public ResponseEntity<AdminCreateResponse> createUser(@Valid @RequestBody CreateUserRequest request,
                                                          CurrentUser currentUser) {
        // Enforce RBAC: SYSTEM_ADMIN only
        RoleSecurity.assertRole(currentUser, UserRole.SYSTEM_ADMIN);

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new StateConflictException("User with email '" + normalizedEmail + "' already exists.");
        }

        // Validate staff insurer linkage constraint (INS-SR-003, SAD §4.3.6, API Contract §5.1)
        if (request.getRole().isStaff()) {
            if (request.getInsurerId() == null || request.getInsurerId() <= 0) {
                throw new ValidationException("insurerId", "Staff role '" + request.getRole() + "' must be linked to a valid insurerId");
            }
            if (!insurerRepository.existsById(request.getInsurerId())) {
                throw new ValidationException("insurerId", "Insurer with id=" + request.getInsurerId() + " does not exist");
            }
        }

        String passwordHash = passwordService.hashPassword(request.getPassword());

        User user = new User(
                request.getName().trim(),
                normalizedEmail,
                request.getPhone().trim(),
                passwordHash,
                request.getRole(),
                request.getRole().isStaff() ? request.getInsurerId() : null
        );

        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AdminCreateResponse(savedUser.getId()));
    }

    /**
     * Creates an insurer organization record (SAD §4.3.6, API Contract §5.2).
     */
    @PostMapping("/insurers")
    public ResponseEntity<AdminCreateResponse> createInsurer(@Valid @RequestBody CreateInsurerRequest request,
                                                            CurrentUser currentUser) {
        // Enforce RBAC: SYSTEM_ADMIN only
        RoleSecurity.assertRole(currentUser, UserRole.SYSTEM_ADMIN);

        String name = request.getName().trim();
        if (insurerRepository.existsByNameIgnoreCase(name)) {
            throw new StateConflictException("Insurer with name '" + name + "' already exists.");
        }

        Insurer insurer = new Insurer(name);
        Insurer savedInsurer = insurerRepository.save(insurer);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AdminCreateResponse(savedInsurer.getId()));
    }
}
