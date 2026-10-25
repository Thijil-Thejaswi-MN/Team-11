package com.team11.insurance.policies;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.filestore.LoadedDocument;
import com.team11.insurance.policies.dto.PolicySummaryResponse;

/**
 * REST Controller for Policy queries and document retrieval (P3-05 / INS-F-024, SAD §4.3.4, p3-contract §3.3).
 * Base path: /policies (under context /api/v1 -> /api/v1/policies).
 */
@RestController
@RequestMapping("/policies")
public class PolicyController {

    private static final Logger log = LoggerFactory.getLogger(PolicyController.class);

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = Objects.requireNonNull(policyService, "policyService must not be null");
    }

    /**
     * Lists policies according to caller's role (Customers see own policies only).
     *
     * @param status optional status filter (e.g. ACTIVE, CANCELLED)
     * @param currentUser currently authenticated user context
     * @return list of policies
     */
    @GetMapping
    public ResponseEntity<List<PolicySummaryResponse>> getPolicies(
            @RequestParam(required = false) PolicyStatus status,
            CurrentUser currentUser) {
        log.debug("GET /policies called: status={}, user={}", status, currentUser);
        List<PolicySummaryResponse> policies = policyService.getPolicies(currentUser, status);
        return ResponseEntity.ok(policies);
    }

    /**
     * Retrieves single policy details with ownership verification.
     *
     * @param id policy primary key
     * @param currentUser currently authenticated user context
     * @return policy details
     */
    @GetMapping("/{id}")
    public ResponseEntity<PolicySummaryResponse> getPolicyById(
            @PathVariable Long id,
            CurrentUser currentUser) {
        log.debug("GET /policies/{} called by user={}", id, currentUser);
        PolicySummaryResponse policy = policyService.getPolicyById(id, currentUser);
        return ResponseEntity.ok(policy);
    }

    /**
     * Streams policy schedule PDF document with ownership verification.
     *
     * @param id policy primary key
     * @param currentUser currently authenticated user context
     * @return PDF binary resource
     */
    @GetMapping("/{id}/document")
    public ResponseEntity<Resource> getPolicyDocument(
            @PathVariable Long id,
            CurrentUser currentUser) {
        log.info("GET /policies/{}/document called by user={}", id, currentUser);
        LoadedDocument loaded = policyService.getPolicyDocument(id, currentUser);

        String filename = loaded.metadata() != null && loaded.metadata().originalFilename() != null
                ? loaded.metadata().originalFilename()
                : "policy-" + id + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(loaded.resource());
    }
}
