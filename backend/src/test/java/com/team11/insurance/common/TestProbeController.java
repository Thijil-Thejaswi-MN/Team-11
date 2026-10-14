package com.team11.insurance.common;

import com.team11.insurance.common.exception.*;
import com.team11.insurance.common.logging.CorrelationIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Controller strictly for testing GlobalExceptionHandler and CorrelationIdFilter.
 */
@RestController
@RequestMapping("/test")
public class TestProbeController {

    public static class SampleValidationDto {
        @NotBlank(message = "applicantName is required")
        private String applicantName;

        @Email(message = "email must be valid format")
        @NotBlank(message = "email is required")
        private String email;

        @Positive(message = "declaredAmount must be greater than zero")
        private BigDecimal declaredAmount;

        public SampleValidationDto() {}

        public SampleValidationDto(String applicantName, String email, BigDecimal declaredAmount) {
            this.applicantName = applicantName;
            this.email = email;
            this.declaredAmount = declaredAmount;
        }

        public String getApplicantName() { return applicantName; }
        public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public BigDecimal getDeclaredAmount() { return declaredAmount; }
        public void setDeclaredAmount(BigDecimal declaredAmount) { this.declaredAmount = declaredAmount; }
    }

    @PostMapping("/validation")
    public ResponseEntity<Map<String, String>> testValidation(@Valid @RequestBody SampleValidationDto dto) {
        return ResponseEntity.ok(Map.of("status", "valid", "name", dto.getApplicantName()));
    }

    @GetMapping("/not-found")
    public void testNotFound() {
        throw new ResourceNotFoundException("Policy", "POL-2026-999");
    }

    @GetMapping("/state-conflict")
    public void testStateConflict() {
        throw new StateConflictException("Cannot process payment for policy in CANCELLED state");
    }

    @GetMapping("/payment-failed")
    public void testPaymentFailed() {
        throw new PaymentFailedException("Payment gateway timeout; transaction may be retried without double charging");
    }

    @GetMapping("/access-denied")
    public void testAccessDenied() {
        throw new AccessDeniedException("Insurer B staff cannot access Insurer A records (INS-SR-004)");
    }

    @GetMapping("/account-locked")
    public void testAccountLocked() {
        throw new AccountLockedException("Account locked after 5 failed login attempts for 15 minutes");
    }

    @GetMapping("/auth-unauthorized")
    public void testUnauthorized() {
        throw new AuthenticationException("Session idle timeout exceeded 15 minutes");
    }

    @GetMapping("/system-error")
    public void testSystemError() {
        throw new RuntimeException("Simulated unexpected internal database failure: SecretPassword=12345");
    }

    @GetMapping("/echo-correlation")
    public ResponseEntity<Map<String, String>> testEchoCorrelation() {
        String mdcCorrelationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        return ResponseEntity.ok(Map.of("correlationId", mdcCorrelationId != null ? mdcCorrelationId : "NONE"));
    }
}
