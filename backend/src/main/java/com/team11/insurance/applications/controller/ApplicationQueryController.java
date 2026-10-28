package com.team11.insurance.applications.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationRepository;
import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.RoleSecurity;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.common.exception.AccessDeniedException;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.filestore.DocumentCategory;
import com.team11.insurance.filestore.IFileStore;
import com.team11.insurance.plans.Plan;
import com.team11.insurance.plans.PlanRepository;
import com.team11.insurance.policies.PolicyRepository;

/**
 * Application query and document upload endpoints supporting the P3-06 customer application screen.
 */
@RestController
@RequestMapping("/applications")
public class ApplicationQueryController {

    private final ApplicationRepository applicationRepository;
    private final ObjectProvider<PlanRepository> planRepositoryProvider;
    private final ObjectProvider<PolicyRepository> policyRepositoryProvider;
    private final ObjectProvider<IFileStore> fileStoreProvider;

    @Autowired
    public ApplicationQueryController(
            ApplicationRepository applicationRepository,
            ObjectProvider<PlanRepository> planRepositoryProvider,
            ObjectProvider<PolicyRepository> policyRepositoryProvider,
            ObjectProvider<IFileStore> fileStoreProvider) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository, "applicationRepository must not be null");
        this.planRepositoryProvider = planRepositoryProvider;
        this.policyRepositoryProvider = policyRepositoryProvider;
        this.fileStoreProvider = fileStoreProvider;
    }

    public record ApplicationSummaryDto(
            Long id,
            Long quoteId,
            Long planId,
            String planName,
            Long insurerId,
            String productType,
            String vehicleRegNo,
            BigDecimal premium,
            ApplicationStatus status,
            String rejectionReason,
            String documentIds,
            int timelineStep,
            boolean policyIssued,
            Instant createdAt
    ) {}

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<ApplicationSummaryDto>> listApplications(CurrentUser currentUser) {
        if (currentUser == null || currentUser.role() == null) {
            throw new AccessDeniedException("Authentication is required.");
        }

        List<ApplicationSummaryDto> items = applicationRepository.findAll()
                .stream()
                .filter(app -> {
                    if (currentUser.role() == UserRole.CUSTOMER) {
                        return Objects.equals(app.getCustomerId(), currentUser.userId());
                    }
                    if (currentUser.role() == UserRole.INSURER_ADMIN || currentUser.role() == UserRole.UNDERWRITER) {
                        return Objects.equals(app.getInsurerId(), currentUser.userId());
                    }
                    return currentUser.role() == UserRole.SYSTEM_ADMIN;
                })
                .sorted(Comparator.comparing(Application::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApplicationSummaryDto> getApplication(
            @PathVariable Long id,
            CurrentUser currentUser) {
        if (currentUser == null || currentUser.role() == null) {
            throw new AccessDeniedException("Authentication is required.");
        }

        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));

        if (currentUser.role() == UserRole.CUSTOMER && !Objects.equals(app.getCustomerId(), currentUser.userId())) {
            throw new AccessDeniedException("You are not authorized to view this application.");
        }
        if ((currentUser.role() == UserRole.INSURER_ADMIN || currentUser.role() == UserRole.UNDERWRITER)
                && !Objects.equals(app.getInsurerId(), currentUser.userId())) {
            throw new AccessDeniedException("You are not authorized to view this application.");
        }

        return ResponseEntity.ok(toDto(app));
    }

    @PostMapping("/documents")
    public ResponseEntity<Map<String, Object>> uploadApplicationDocument(
            @RequestParam("file") MultipartFile file,
            CurrentUser currentUser) {
        RoleSecurity.assertRole(currentUser, UserRole.CUSTOMER);
        IFileStore fileStore = fileStoreProvider != null ? fileStoreProvider.getIfAvailable() : null;
        if (fileStore == null) {
            throw new ResourceNotFoundException("File storage service is not available.");
        }
        Long docId = fileStore.put(file, DocumentCategory.POLICY, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "documentId", docId,
                "filename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf"
        ));
    }

    private ApplicationSummaryDto toDto(Application app) {
        String planName = "Plan #" + app.getPlanId();
        PlanRepository planRepo = planRepositoryProvider != null ? planRepositoryProvider.getIfAvailable() : null;
        if (planRepo != null && app.getPlanId() != null) {
            planName = planRepo.findById(app.getPlanId()).map(Plan::getName).orElse(planName);
        }

        PolicyRepository policyRepo = policyRepositoryProvider != null ? policyRepositoryProvider.getIfAvailable() : null;
        boolean policyIssued = policyRepo != null && app.getId() != null && policyRepo.existsByApplicationId(app.getId());

        int step = computeTimelineStep(app.getStatus(), policyIssued);

        return new ApplicationSummaryDto(
                app.getId(),
                app.getQuoteId(),
                app.getPlanId(),
                planName,
                app.getInsurerId(),
                app.getProductType(),
                app.getVehicleRegNo(),
                app.getPremium(),
                app.getStatus(),
                app.getRejectionReason(),
                app.getDocumentIds(),
                step,
                policyIssued,
                app.getCreatedAt()
        );
    }

    private int computeTimelineStep(ApplicationStatus status, boolean policyIssued) {
        if (policyIssued) {
            return 5;
        }
        if (status == null) {
            return 1;
        }
        return switch (status) {
            case SUBMITTED -> 2;
            case REFERRED, REJECTED -> 3;
            case APPROVED -> 4;
            case PAID -> 5;
        };
    }
}
