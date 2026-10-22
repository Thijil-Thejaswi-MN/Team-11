package com.team11.insurance.payments;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.payments.dto.ApplicationPaymentSummary;
import com.team11.insurance.payments.dto.PaymentInitiationRequest;
import com.team11.insurance.payments.dto.PaymentResponse;

import jakarta.validation.Valid;

/**
 * REST Controller for Payment processing (INS-F-009, P4-17, p3-contract §3.2).
 * <p>
 * Mapped under /payments (servlet context-path /api/v1 gives /api/v1/payments).
 * <p>
 * Security Invariant: The client browser sends ONLY {@code applicationId}.
 * Never accepts amount or idempotency key from client.
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = Objects.requireNonNull(paymentService, "paymentService must not be null");
    }

    /**
     * Retrieves application details and verified payable amount for the payment screen.
     *
     * @param applicationId the application ID
     * @param currentUser currently authenticated user context
     * @return ApplicationPaymentSummary
     */
    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<ApplicationPaymentSummary> getApplicationSummary(
            @PathVariable Long applicationId,
            CurrentUser currentUser) {
        log.debug("Fetching application summary for payment: applicationId={}, user={}", applicationId, currentUser);
        ApplicationPaymentSummary summary = paymentService.getApplicationSummary(applicationId, currentUser);
        return ResponseEntity.ok(summary);
    }

    /**
     * Initiates a payment for an approved application.
     *
     * @param request the payment initiation request containing ONLY applicationId
     * @param currentUser currently authenticated user context
     * @return PaymentResponse on successful charge and policy issuance
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody PaymentInitiationRequest request,
            CurrentUser currentUser) {
        log.info("Processing payment initiation for applicationId={}, user={}", request.applicationId(), currentUser);
        PaymentResponse response = paymentService.processPayment(request.applicationId(), currentUser);
        return ResponseEntity.ok(response);
    }
}
