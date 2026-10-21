package com.team11.insurance.quotes;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.RoleSecurity;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.quotes.dto.MotorQuoteRequest;
import com.team11.insurance.quotes.dto.QuoteResponse;

/**
 * REST controller for quote generation endpoints (INS-F-005, INS-F-006, P1-06).
 * Mapped under /quotes (servlet context-path /api/v1 gives /api/v1/quotes).
 */
@RestController
@RequestMapping("/quotes")
public class QuoteController {

    private static final Logger log = LoggerFactory.getLogger(QuoteController.class);

    private final MotorQuoteService motorQuoteService;

    public QuoteController(MotorQuoteService motorQuoteService) {
        this.motorQuoteService = Objects.requireNonNull(motorQuoteService, "motorQuoteService must not be null");
    }

    /**
     * Calculates a motor insurance quote for two-wheelers or four-wheelers.
     * Enforces Customer role and executes server-side rating rules.
     *
     * @param request Quote calculation parameters
     * @param currentUser Authenticated customer context
     * @return QuoteResponse with quote ID, total premium, breakdown, and validity
     */
    @PostMapping("/motor")
    public ResponseEntity<QuoteResponse> createMotorQuote(
            @RequestBody MotorQuoteRequest request,
            CurrentUser currentUser) {

        RoleSecurity.assertRole(currentUser, UserRole.CUSTOMER);

        log.debug("Received motor quote request from customerId={}: planId={}, idv={}",
                currentUser.getUserId(), request != null ? request.getPlanId() : null, request != null ? request.getIdv() : null);

        QuoteResponse response = motorQuoteService.calculateMotorQuote(request, currentUser.getUserId());

        return ResponseEntity.ok(response);
    }
}
