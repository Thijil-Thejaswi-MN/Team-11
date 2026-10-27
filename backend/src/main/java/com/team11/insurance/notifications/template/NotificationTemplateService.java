package com.team11.insurance.notifications.template;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.team11.insurance.notifications.NotificationEvent;

/**
 * Service managing template validation and rendering for the authoritative 16-event catalogue
 * (Component C9 - SAD §4.1.7; STP TC-Ntf-01; P4 Contract Section 4.2).
 */
@Service
public class NotificationTemplateService {

    private static final Logger log = LoggerFactory.getLogger(NotificationTemplateService.class);

    /**
     * Validates and renders the notification payload for the given event and parameter map.
     *
     * @param event  the canonical notification event
     * @param params dynamic key-value parameters provided by the business workflow
     * @return rendered {@link NotificationPayload} with subject, email body, and SMS text
     * @throws NotificationValidationException if required parameters are missing or invalid
     */
    public NotificationPayload render(NotificationEvent event, Map<String, String> params) {
        if (event == null) {
            throw new NotificationValidationException("NotificationEvent cannot be null");
        }
        Map<String, String> safeParams = params != null ? params : Map.of();

        return switch (event) {
            case APPLICATION_APPROVED -> renderApplicationApproved(safeParams);
            case APPLICATION_REFERRED -> renderApplicationReferred(safeParams);
            case APPLICATION_REJECTED -> renderApplicationRejected(safeParams);
            case PAYMENT_FAILED -> renderPaymentFailed(safeParams);
            case POLICY_ISSUED -> renderPolicyIssued(safeParams);
            case POLICY_RENEWED -> renderPolicyRenewed(safeParams);
            case RENEWAL_REMINDER -> renderRenewalReminder(safeParams);
            case CANCELLATION_REQUESTED -> renderCancellationRequested(safeParams);
            case CANCELLATION_APPROVED -> renderCancellationApproved(safeParams);
            case CANCELLATION_REJECTED -> renderCancellationRejected(safeParams);
            case CLAIM_RECEIVED -> renderClaimReceived(safeParams);
            case CLAIM_DOCUMENTS_REQUESTED -> renderClaimDocumentsRequested(safeParams);
            case CLAIM_APPROVED -> renderClaimApproved(safeParams);
            case CLAIM_REJECTED -> renderClaimRejected(safeParams);
            case CLAIM_SETTLED -> renderClaimSettled(safeParams);
            case CLAIM_CLOSED -> renderClaimClosed(safeParams);
        };
    }

    private NotificationPayload renderApplicationApproved(Map<String, String> p) {
        String appId = requireParam(p, "applicationId", "appId", "id");
        String planName = p.getOrDefault("planName", "Your Insurance Plan");
        String premium = p.getOrDefault("premium", "the calculated premium");

        String subject = "Insurance Application Approved - #" + appId;
        String email = "Dear Customer,\n\nWe are pleased to inform you that your insurance application #" + appId +
                " (" + planName + ") has been APPROVED.\nPayable Premium: " + premium +
                ".\nPlease log in to your portal and complete the payment to activate your policy.\n\nTeam-11 Insurance";
        String sms = "Your insurance application #" + appId + " has been APPROVED. Please log in to complete payment and activate your policy.";

        return new NotificationPayload(subject, email, sms);
    }

    private NotificationPayload renderApplicationReferred(Map<String, String> p) {
        String appId = requireParam(p, "applicationId", "appId", "id");
        String reason = p.getOrDefault("reason", "Requires manual underwriter assessment");

        String subject = "Insurance Application Under Review - #" + appId;
        String email = "Dear Customer,\n\nYour insurance application #" + appId +
                " has been referred for manual underwriter review.\nReason: " + reason +
                ".\nOur team is reviewing your documents and will update you shortly.\n\nTeam-11 Insurance";
        String sms = "Application #" + appId + " has been referred for underwriter review (Reason: " + reason + "). We will notify you once completed.";

        return new NotificationPayload(subject, email, sms);
    }

    private NotificationPayload renderApplicationRejected(Map<String, String> p) {
        String appId = requireParam(p, "applicationId", "appId", "id");
        String reason = requireParam(p, "reason", "rejectionReason");

        String subject = "Insurance Application Update - #" + appId;
        String email = "Dear Customer,\n\nThank you for applying with Team-11 Insurance. After review, your application #" +
                appId + " could not be approved at this time.\nReason: " + reason +
                ".\nPlease contact customer support if you have any questions.\n\nTeam-11 Insurance";
        String sms = "Insurance application #" + appId + " was declined. Reason: " + reason + ". Contact support for details.";

        return new NotificationPayload(subject, email, sms);
    }

    private NotificationPayload renderPaymentFailed(Map<String, String> p) {
        String identifier = getParam(p, "applicationId", "policyNo", "appId", "id");
        if (identifier == null) {
            throw new NotificationValidationException("PAYMENT_FAILED requires 'applicationId' or 'policyNo'");
        }
        String amount = p.getOrDefault("amount", "");
        String reason = p.getOrDefault("reason", "Card/Bank declined transaction");

        String subject = "Payment Failed - Action Required for " + identifier;
        String email = "Dear Customer,\n\nYour premium payment for " + identifier + " was unsuccessful.\nReason: " + reason +
                (amount.isEmpty() ? "" : "\nAmount: " + amount) +
                ".\nRetry is allowed. Please visit your payment page to re-attempt payment with valid payment details.\n\nTeam-11 Insurance";
        String sms = "Payment failed for " + identifier + ". Reason: " + reason + ". Please visit your customer portal to retry payment.";

        return new NotificationPayload(subject, email, sms);
    }

    private NotificationPayload renderPolicyIssued(Map<String, String> p) {
        String policyNo = requireParam(p, "policyNo", "policyId");
        String planName = p.getOrDefault("planName", "Comprehensive Insurance");
        String startDate = p.getOrDefault("startDate", "Today");
        String endDate = p.getOrDefault("endDate", "1 year from start date");
        String amount = p.getOrDefault("amount", p.getOrDefault("premium", ""));

        String subject = "Policy Issued - Policy #" + policyNo;
        String email = "Dear Customer,\n\nCongratulations! Your policy #" + policyNo + " (" + planName +
                ") has been successfully issued and is now ACTIVE.\nCoverage period: " + startDate + " to " + endDate +
                (amount.isEmpty() ? "" : "\nPremium Amount: " + amount) +
                ".\nYour policy document is available for download in your dashboard.\n\nTeam-11 Insurance";
        String sms = "Your insurance policy #" + policyNo + " is now ACTIVE!" +
                (amount.isEmpty() ? "" : " Premium: " + amount + ".") +
                " Coverage started " + startDate + ". Thank you for choosing Team-11.";

        return new NotificationPayload(subject, email, sms);
    }

    private NotificationPayload renderPolicyRenewed(Map<String, String> p) {
        String policyNo = requireParam(p, "policyNo", "policyId");
        String startDate = p.getOrDefault("startDate", "Renewal start date");
        String endDate = p.getOrDefault("endDate", "Renewal end date");
        String amount = p.getOrDefault("amount", p.getOrDefault("premium", ""));

        String subject = "Policy Renewed Successfully - #" + policyNo;
        String email = "Dear Customer,\n\nYour insurance policy #" + policyNo +
                " has been successfully renewed.\nNew term: " + startDate + " to " + endDate +
                (amount.isEmpty() ? "" : "\nPremium Amount: " + amount) +
                ".\nThank you for continuing your protection with us.\n\nTeam-11 Insurance";
        String sms = "Policy #" + policyNo + " has been successfully renewed. Your coverage continues without interruption.";

        return new NotificationPayload(subject, email, sms);
    }

    private NotificationPayload renderRenewalReminder(Map<String, String> p) {
        String policyNo = requireParam(p, "policyNo", "policyId");
        String daysRemaining = p.getOrDefault("daysRemaining", "upcoming");
        String expiryDate = p.getOrDefault("expiryDate", "");

        String subject = "Renewal Reminder: Policy #" + policyNo + " (" + daysRemaining + " days remaining)";
        String email = "Dear Customer,\n\nThis is a friendly reminder that your policy #" + policyNo +
                " is scheduled to expire in " + daysRemaining + " days" +
                (expiryDate.isEmpty() ? "" : " on " + expiryDate) +
                ".\nPlease renew your policy before expiration to maintain uninterrupted coverage.\n\nTeam-11 Insurance";
        String sms = "Reminder: Policy #" + policyNo + " expires in " + daysRemaining + " days. Renew now to avoid lapse in coverage.";

        return new NotificationPayload(subject, email, sms);
    }

    private NotificationPayload renderCancellationRequested(Map<String, String> p) {
        String policyNo = requireParam(p, "policyNo", "policyId");
        String reason = p.getOrDefault("reason", "Customer requested cancellation");

        String subject = "Policy Cancellation Request Received - #" + policyNo;
        String email = "Dear Customer,\n\nWe have received your cancellation request for policy #" + policyNo +
                ".\nReason: " + reason +
                ".\nOur operations team will review your request and calculate any applicable pro-rata refund.\n\nTeam-11 Insurance";
        String sms = "Cancellation request received for policy #" + policyNo + ". It is being reviewed by our administration team.";

        return new NotificationPayload(subject, email, sms);
    }

    private NotificationPayload renderCancellationApproved(Map<String, String> p) {
        String policyNo = requireParam(p, "policyNo", "policyId");
        String refundAmount = p.getOrDefault("refundAmount", "applicable pro-rata amount");

        String subject = "Policy Cancellation Approved - #" + policyNo;
        String email = "Dear Customer,\n\nYour cancellation request for policy #" + policyNo +
                " has been APPROVED.\nRefund amount: " + refundAmount +
                ".\nThe refund will be credited to your original payment method in accordance with policy terms.\n\nTeam-11 Insurance";
        String sms = "Policy #" + policyNo + " cancellation approved. Refund of " + refundAmount + " will be credited to your account.";

        return new NotificationPayload(subject, email, sms);
    }

    private NotificationPayload renderCancellationRejected(Map<String, String> p) {
        String policyNo = requireParam(p, "policyNo", "policyId");
        String reason = requireParam(p, "reason", "rejectionReason");

        String subject = "Policy Cancellation Declined - #" + policyNo;
        String email = "Dear Customer,\n\nYour cancellation request for policy #" + policyNo +
                " was reviewed and declined.\nReason: " + reason +
                ".\nYour policy remains active. Please contact support for assistance.\n\nTeam-11 Insurance";
        String sms = "Cancellation request for policy #" + policyNo + " was declined. Reason: " + reason + ". Policy remains active.";

        return new NotificationPayload(subject, email, sms);
    }

    private NotificationPayload renderClaimReceived(Map<String, String> p) {
        String claimNo = requireParam(p, "claimNo", "claimId");
        String policyNo = p.getOrDefault("policyNo", p.getOrDefault("policyId", ""));

        String subject = "Claim Received - Claim #" + claimNo;
        String email = "Dear Customer,\n\nWe have received your claim filing #" + claimNo +
                (policyNo.isEmpty() ? "" : " under policy #" + policyNo) +
                ".\nStatus: SUBMITTED / DOCUMENT VERIFICATION.\n" +
                "Our claims team is verifying the details and documents. You can track your claim status anytime online.\n\nTeam-11 Insurance";
        String sms = "Claim #" + claimNo + (policyNo.isEmpty() ? "" : " for policy #" + policyNo) +
                " has been received and is under document verification.";

        return new NotificationPayload(subject, email, sms);
    }

    private NotificationPayload renderClaimDocumentsRequested(Map<String, String> p) {
        String claimNo = requireParam(p, "claimNo", "claimId");
        String documents = requireParam(p, "requiredDocuments", "reason", "documents");
        String deadlineDays = p.getOrDefault("deadlineDays", "15");

        String subject = "Action Required: Additional Documents for Claim #" + claimNo;
        String email = "Dear Customer,\n\nOur claims officer requires additional documentation to process claim #" + claimNo +
                ".\nRequired documents: " + documents +
                ".\nPlease upload these documents within " + deadlineDays + " days to avoid automatic claim closure.\n\nTeam-11 Insurance";
        String sms = "Documents required for claim #" + claimNo + ": " + documents + ". Please submit within " + deadlineDays + " days.";

        return new NotificationPayload(subject, email, sms);
    }

    private NotificationPayload renderClaimApproved(Map<String, String> p) {
        String claimNo = requireParam(p, "claimNo", "claimId");
        String approvedAmount = p.getOrDefault("approvedAmount", p.getOrDefault("amount", p.getOrDefault("payoutAmount", "the assessed claim amount")));

        String subject = "Claim Approved - #" + claimNo;
        String email = "Dear Customer,\n\nGood news! Your claim #" + claimNo +
                " has been APPROVED for settlement.\nApproved Amount: " + approvedAmount +
                ".\nPayment processing has been initiated.\n\nTeam-11 Insurance";
        String sms = "Claim #" + claimNo + " has been APPROVED for " + approvedAmount + ". Payout processing initiated.";

        return new NotificationPayload(subject, email, sms);
    }

    private NotificationPayload renderClaimRejected(Map<String, String> p) {
        String claimNo = requireParam(p, "claimNo", "claimId");
        String reason = requireParam(p, "reason", "rejectionReason");

        String subject = "Claim Decision: Declined - #" + claimNo;
        String email = "Dear Customer,\n\nYour claim #" + claimNo +
                " has been reviewed by the claims assessment committee and declined.\nReason: " + reason +
                ".\nIf you have further evidence or wish to appeal, please contact our claims desk.\n\nTeam-11 Insurance";
        String sms = "Claim #" + claimNo + " was rejected. Reason: " + reason + ". Contact claims support for details.";

        return new NotificationPayload(subject, email, sms);
    }

    private NotificationPayload renderClaimSettled(Map<String, String> p) {
        String claimNo = requireParam(p, "claimNo", "claimId");
        String settledAmount = requireParam(p, "settledAmount", "amount");
        String txnRef = p.getOrDefault("txnRef", p.getOrDefault("payoutReference", "N/A"));

        String subject = "Claim Settled - Payout Processed (#" + claimNo + ")";
        String email = "Dear Customer,\n\nYour claim #" + claimNo + " has been SETTLED.\nSettled Payout: " +
                settledAmount + "\nTransaction Reference: " + txnRef +
                ".\nThe funds have been transferred to your registered bank account.\n\nTeam-11 Insurance";
        String sms = "Claim #" + claimNo + " settled! Payout of " + settledAmount + " processed (Ref: " + txnRef + ").";

        return new NotificationPayload(subject, email, sms);
    }

    private NotificationPayload renderClaimClosed(Map<String, String> p) {
        String claimNo = requireParam(p, "claimNo", "claimId");
        String reason = p.getOrDefault("reason", "No response to document request within 15 days");

        String subject = "Claim Closed - #" + claimNo;
        String email = "Dear Customer,\n\nYour claim #" + claimNo +
                " has been CLOSED.\nReason: " + reason +
                ".\nYou may contact customer support or file a fresh claim if appropriate.\n\nTeam-11 Insurance";
        String sms = "Claim #" + claimNo + " was closed due to: " + reason + ". Contact support for inquiries.";

        return new NotificationPayload(subject, email, sms);
    }

    private String requireParam(Map<String, String> params, String... keys) {
        String val = getParam(params, keys);
        if (val == null || val.trim().isEmpty()) {
            throw new NotificationValidationException("Missing required parameter: " + String.join(" or ", keys));
        }
        return val.trim();
    }

    private String getParam(Map<String, String> params, String... keys) {
        for (String k : keys) {
            String v = params.get(k);
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return null;
    }
}
