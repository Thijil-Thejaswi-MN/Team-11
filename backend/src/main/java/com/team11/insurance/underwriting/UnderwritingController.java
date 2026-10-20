package com.team11.insurance.underwriting;

import java.util.List;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationRepository;
import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.RoleSecurity;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.underwriting.dto.UnderwriterDecisionRequest;
import com.team11.insurance.underwriting.dto.UnderwritingDecisionResponse;

import jakarta.validation.Valid;

/**
 * REST controller for Underwriting operations (INS-F-010, INS-F-011, SAD §4.3.7).
 * <p>
 * Base path: {@code /applications} (under context path {@code /api/v1}).
 * Provides underwriter queue access and manual review decision recording.
 */
@RestController
@RequestMapping("/applications")
public class UnderwritingController {

    private final IUnderwriting underwriting;
    private final ApplicationRepository applicationRepository;

    public UnderwritingController(IUnderwriting underwriting, ApplicationRepository applicationRepository) {
        this.underwriting = Objects.requireNonNull(underwriting, "underwriting must not be null");
        this.applicationRepository = Objects.requireNonNull(applicationRepository, "applicationRepository must not be null");
    }

    /**
     * Retrieves all applications currently in REFERRED status pending manual underwriting assessment.
     * Enforces RBAC (UNDERWRITER, INSURER_ADMIN, SYSTEM_ADMIN) and tenant isolation.
     */
    @GetMapping("/referred")
    public ResponseEntity<List<Application>> getReferredApplications(CurrentUser currentUser) {
        RoleSecurity.assertRole(currentUser, UserRole.UNDERWRITER, UserRole.INSURER_ADMIN, UserRole.SYSTEM_ADMIN);

        List<Application> referredApplications;
        if (currentUser.isSystemAdmin()) {
            referredApplications = applicationRepository.findByStatus(ApplicationStatus.REFERRED);
        } else {
            referredApplications = applicationRepository.findByInsurerIdAndStatus(
                    currentUser.getInsurerId(),
                    ApplicationStatus.REFERRED
            );
        }

        return ResponseEntity.ok(referredApplications);
    }

    /**
     * Records a manual decision (APPROVE or REJECT) by an underwriter on a referred application.
     */
    @PostMapping("/{id}/decision")
    public ResponseEntity<UnderwritingDecisionResponse> recordDecision(
            @PathVariable("id") Long id,
            @Valid @RequestBody UnderwriterDecisionRequest request,
            CurrentUser currentUser) {
        RoleSecurity.assertRole(currentUser, UserRole.UNDERWRITER, UserRole.INSURER_ADMIN);

        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", id));

        // Enforce insurer tenant isolation (INS-SR-003)
        RoleSecurity.assertInsurerAccess(currentUser, application.getInsurerId());

        underwriting.recordManualDecision(id, request.getDecision(), request.getReason(), currentUser.getUserId());

        // Refresh application state to return updated status
        Application updated = applicationRepository.findById(id).orElse(application);

        return ResponseEntity.ok(new UnderwritingDecisionResponse(
                id,
                updated.getStatus(),
                request.getReason()
        ));
    }
}
