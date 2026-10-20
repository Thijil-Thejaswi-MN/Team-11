package com.team11.insurance.applications.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.applications.dto.SubmitApplicationRequest;
import com.team11.insurance.applications.dto.SubmitApplicationResponse;
import com.team11.insurance.applications.service.ApplicationSubmissionService;
import com.team11.insurance.applications.service.SubmissionResult;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.RoleSecurity;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.common.exception.ValidationException;

import jakarta.validation.Valid;

/**
 * Application endpoints of Quote & Purchase (INS-F-007, SAD 4.3.3).
 * Base path /applications (served under the servlet context path /api/v1).
 */
@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationSubmissionService submissionService;

    public ApplicationController(ApplicationSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /**
     * Submit an application for a quote. Customers only; the identity comes from the server-side session.
     * 201 Approved or Referred. For a Rejected application the record is already saved by the service and the
     * answer is 422 VALID-422 with the reason; throwing here, outside the service's transactions, cannot roll it back.
     */
    @PostMapping
    public ResponseEntity<SubmitApplicationResponse> submit(CurrentUser currentUser,
                                                            @Valid @RequestBody SubmitApplicationRequest request) {
        RoleSecurity.assertRole(currentUser, UserRole.CUSTOMER);
        SubmissionResult result = submissionService.submit(currentUser, request);
        if (result.status() == ApplicationStatus.REJECTED) {
            String reason = (result.reason() == null || result.reason().isBlank())
                    ? "No reason given" : result.reason();
            throw new ValidationException("Application rejected", reason);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SubmitApplicationResponse.of(result.applicationId(), result.status()));
    }
}
