# P4 Contract Draft (P4-03) — Shared Interfaces, Storage, and Test Clock

**Owner:** Person 4 — Raghunandan Kotary (Developer / Environment Owner)  
**Status:** Draft v0.1 — Pending Team Sign-Off (17 October 2026)  
**Tracking Issue:** [#127](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/127)  
**Source of Truth:** SAD v1.0/v1.1 (Sections 3.4, 4.1.2, 4.1.5, 4.1.6, 4.3.3, 4.3.5, 4.4; ADR-005, ADR-007, ADR-008), SRS v1.0, STP v1.0 §8, P3 Contract Draft v0.3 (`docs/contracts/p3-contract.md`), and P1 API Contract (`docs/api-contract/01-quotes-underwriting-claims-audit.md`).  
**Important Notice:** This document defines the shared interface contracts, data models, error semantics, and test-clock controls required across modules before P4-05 (Test Infrastructure) and P4-06 (Interface Stubs) can be implemented. No implementation or team approval is claimed until explicitly signed off by all four team members.

---

## 1. Scope and Traceability

| Requirement | Module / Component | Responsibility | Relevant Test Cases |
|---|---|---|---|
| **INS-F-008** | C8 Payment Adapter | Premium charge, gateway abstraction, retry, idempotency | TC-Pay-01, TC-Pay-02, TC-Pay-03 |
| **INS-F-012** | C7/C10 Claims Workflow | Claim submission, policy validation, document linking | TC-Clm-01 |
| **INS-F-013** | C7/C10 Claims Workflow | Claim state transitions, duplicate detection, stale close | TC-Clm-02, TC-Clm-05 |
| **INS-F-014** | C7/C10 Claims Workflow | Claims verification, assessment, approval/rejection, payout | TC-Clm-03, TC-Clm-06 |
| **INS-F-015** | C7/C10 Claims Workflow | Customer claim status tracking | TC-Clm-04 |
| **INS-F-016** | C9 Notification Service | Event-driven status notifications (Email/SMS) | TC-Ntf-01 |
| **INS-F-017** | C9 Notification Service | Automated renewal reminders (30, 15, 1 days) | TC-Ren-01 |
| **INS-SR-004** | C12 File Store | Document encryption at rest (AES-256) | TC-Sec-04, TC-Sec-11 |
| **INS-SR-007** | C12 File Store | Secure upload: max 5 MB, PDF/JPG/PNG allowlist, traversal check | TC-Sec-07, TC-KYC-01 |
| **P4-03 / STP §8**| Test Infrastructure | Injected `Clock`, virtual time control endpoint (`POST /api/v1/test/clock`) | Time-travel testing for TC-Ren-01, TC-Ren-02, TC-Can-01, TC-Can-02 |

---

## 2. Shared Conventions

1. **Package Namespace:**
   - Payments: `com.team11.insurance.payments`
   - Notifications: `com.team11.insurance.notifications`
   - Claims: `com.team11.insurance.claims`
   - File Storage: `com.team11.insurance.filestore`
   - Clock / Time: `com.team11.insurance.common.time`
2. **Standard API Error Responses:**
   All public endpoints adhere to the established backend error structure:
   ```json
   {
     "timestamp": "2026-10-17T10:00:00Z",
     "status": 402,
     "errorCode": "PAY-402",
     "message": "Payment processing failed",
     "details": [{ "field": "payment", "issue": "Card declined by issuer" }],
     "correlationId": "c0a801-12345-67890"
   }
   ```
   Standard error codes: `VALID-422`, `AUTH-401`, `ACCESS-403`, `NOTFOUND-404`, `STATE-409`, `PAY-402`, `SYS-500`.
3. **Identity and Security Scoping:**
   - User identity resolved from `CurrentUser { userId, role, insurerId }`.
   - Insurer staff (`INSURER_ADMIN`, `UNDERWRITER`, `CLAIMS_OFFICER`) are strictly isolated to records where `entity.insurerId == currentUser.insurerId`.
   - Customers (`CUSTOMER`) are strictly isolated to records where `entity.customerId == currentUser.userId`.
4. **Time Invariant:**
   - Domain logic **must never** call `System.currentTimeMillis()`, `LocalDate.now()`, or `Instant.now()` without passing an injected `java.time.Clock`.
   - All temporal computations (policy start/end dates, 15-day free-look window, 30/15/1-day renewal reminders, 30-day stale claim closure) read `clock`.

---

## 3. Section A — Payment Service Contract (`IPaymentService`)

Component C8 acts as an adapter isolating core insurance workflows from third-party payment gateways (Stripe / Razorpay / Mock Gateway).

### 3.1 Java Interface Definition

```java
package com.team11.insurance.payments;

import java.math.BigDecimal;

/**
 * Payment Adapter Interface (Component C8 - SAD Section 3.4 & ADR-007).
 * Consumed by: Quote & Purchase (C5/C7), Policy Renewal/Cancellation (C7), Claims Workflow (C7/C10).
 */
public interface IPaymentService {

    /**
     * Charges a customer for an approved application premium or renewal.
     *
     * @param applicationId  the ID of the insurance application being purchased
     * @param amount         monetary amount in INR (whole rupees, >= 1)
     * @param idempotencyKey server-generated key: "applicationId-attemptNo"
     * @return PaymentResult indicating SUCCESS or FAILED outcome
     * @throws PaymentInfrastructuralException if database/gateway cannot be reached after retries
     */
    PaymentResult charge(long applicationId, BigDecimal amount, String idempotencyKey);

    /**
     * Refunds a customer following an approved policy cancellation.
     *
     * @param policyId       the ID of the cancelled policy
     * @param amount         refund amount calculated per STP test oracle (Rs 0 skips gateway)
     * @param idempotencyKey server-generated key: "cancel-policyId-requestId"
     * @param reason         cancellation justification recorded for audit
     * @return PaymentResult indicating SUCCESS or FAILED outcome
     */
    PaymentResult refund(long policyId, BigDecimal amount, String idempotencyKey, String reason);

    /**
     * Disburses an approved claim settlement to the customer.
     *
     * @param claimId            the ID of the approved claim
     * @param assessedAmount     the approved settlement amount in INR
     * @param idempotencyKey     server-generated key: "claim-claimId-payout"
     * @param destinationAccount masked bank account / UPI ID
     * @return PaymentResult indicating SUCCESS or FAILED outcome
     */
    PaymentResult payout(long claimId, BigDecimal assessedAmount, String idempotencyKey, String destinationAccount);
}
```

### 3.2 Payment Data Models

```java
public record PaymentResult(
    Long paymentId,
    PaymentStatus status,
    String idempotencyKey,
    String transactionReference,
    BigDecimal amount,
    PaymentType type,            // CHARGE, REFUND, PAYOUT
    Instant processedAt,
    String failureReason,        // null on SUCCESS
    String errorCode             // null on SUCCESS, e.g. "PAY-402"
) {
    public static PaymentResult success(Long paymentId, String key, String ref, BigDecimal amount, PaymentType type) {
        return new PaymentResult(paymentId, PaymentStatus.SUCCESS, key, ref, amount, type, Instant.now(), null, null);
    }

    public static PaymentResult failed(Long paymentId, String key, BigDecimal amount, PaymentType type, String reason, String code) {
        return new PaymentResult(paymentId, PaymentStatus.FAILED, key, null, amount, type, Instant.now(), reason, code);
    }
}

public enum PaymentStatus {
    SUCCESS,
    FAILED,
    PENDING
}

public enum PaymentType {
    CHARGE,
    REFUND,
    PAYOUT
}
```

### 3.3 Transaction & Idempotency Rules (ADR-007)

1. **Idempotency Key Generation:**
   - Application purchase: `applicationId + "-" + attemptNo` (e.g. `7001-1`, `7001-2`).
   - The frontend **never** generates or supplies the idempotency key; it is minted strictly by the backend controller.
2. **Duplicate Detection:**
   - The payment table contains a `UNIQUE(idempotency_key)` database constraint.
   - If `charge()` is called with an existing `idempotencyKey`, the adapter inspects the prior record:
     - If previous status is `SUCCESS`, it returns the existing `PaymentResult` immediately without calling the gateway.
     - If previous status is `FAILED`, subsequent attempts increment `attemptNo` (e.g. `7001-2`), allowing a clean retry with a fresh key.
3. **Autonomous Transaction Scope (`REQUIRES_NEW`):**
   - Payment persistence executes in an isolated transaction so that payment records and audit entries are preserved even if downstream policy issuance fails.
4. **Retry Behavior (INS-F-008 / SAD §3.4):**
   - For network timeouts or 5xx gateway errors, the adapter retries up to 3 times with exponential backoff (1s, 2s, 4s) using the **exact same** `idempotencyKey`.
   - Business rejections (insufficient funds, invalid card, expired card) return `FAILED` immediately without retry.
5. **Zero-Amount Refund Rule (P3 Contract §3.5):**
   - If the calculated refund amount is `BigDecimal.ZERO` (e.g. cancellation requested after a claim was paid), the adapter records a zero-amount `REFUND` record marked `SUCCESS` without making any external payment gateway call.

---

## 4. Section B — Notification Service Contract (`INotify`)

Component C9 delivers asynchronous notifications across Email and SMS channels with multi-channel fallback and dead-letter audit logging (ADR-005).

### 4.1 Java Interface Definition

```java
package com.team11.insurance.notifications;

import java.util.Map;

/**
 * Notification Service Interface (Component C9 - SAD Section 3.4 & ADR-005).
 * Consumed by: Applications (C7), Payments (C8), Claims (C7/C10), Renewal Scheduler (C7/C9).
 */
public interface INotify {

    /**
     * Sends an event notification synchronously or schedules it asynchronously.
     * Guaranteed non-blocking to the caller's database transaction.
     *
     * @param event the structured notification event
     */
    void send(NotificationEvent event);
}
```

### 4.2 Notification Event Catalogue and Payloads

```java
public record NotificationEvent(
    NotificationType eventType,
    Long recipientUserId,
    String recipientEmail,
    String recipientPhone,
    NotificationChannel channel,    // EMAIL, SMS, IN_APP, ALL
    String subject,
    Map<String, Object> templateParams,
    String correlationId
) { }

public enum NotificationChannel {
    EMAIL,
    SMS,
    IN_APP,
    ALL
}

public enum NotificationType {
    // Application Lifecycle
    APPLICATION_SUBMITTED,
    APPLICATION_APPROVED,
    APPLICATION_REFERRED,
    APPLICATION_REJECTED,

    // Payment Lifecycle
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,

    // Policy Lifecycle
    POLICY_ISSUED,
    POLICY_RENEWED,
    RENEWAL_REMINDER,

    // Cancellation Lifecycle
    CANCELLATION_REQUESTED,
    CANCELLATION_APPROVED,
    CANCELLATION_REJECTED,

    // Claims Lifecycle
    CLAIM_SUBMITTED,
    CLAIM_DOCUMENTS_REQUIRED,
    CLAIM_APPROVED,
    CLAIM_REJECTED,
    CLAIM_SETTLED,
    CLAIM_CLOSED_STALE
}
```

### 4.3 Persistence and Delivery Semantics

1. **Outbox Pattern / Asynchronous Execution:**
   - Every `send()` call persists a record into `notification_outbox` (`recipient`, `type`, `channel`, `payload_json`, `status: PENDING`, `created_at`).
   - Delivery is handled asynchronously (`@Async("notificationExecutor")`), guaranteeing that a third-party email/SMS provider latency or failure **never blocks or rolls back** core business transactions.
2. **Retry and Failure Handling:**
   - Transient failures (SMTP disconnect, provider 5xx) retry up to 3 times with exponential backoff (2s, 10s, 30s).
   - If all retries fail, status moves to `FAILED` and an entry is logged in the dead-letter log. No exception is rethrown to the caller.
3. **Template Engine:**
   - Renders messages using Mustache/Thymeleaf templates populated by `templateParams` (e.g. `policyNo`, `claimNo`, `amount`, `customerName`, `expiryDate`).

---

## 5. Section C — Claims Workflow Contract (`IClaimService`)

Component C7/C10 manages the end-to-end claims lifecycle, state machine enforcement (ADR-008), duplicate detection, and external cross-module queries.

### 5.1 Java Interface Definition

```java
package com.team11.insurance.claims;

import com.team11.insurance.auth.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Claims Workflow Interface (Component C7/C10 - SAD 3.4, 4.1.4, 4.3.5 & ADR-008).
 * Consumed by: Policy Cancellation (C7 - hasPaidClaim), REST API Controllers, Claims Officers.
 */
public interface IClaimService {

    /**
     * Checks if any claim has been paid on a policy.
     * Required by P3 cancellation module (SAD §4.1.5, p3-contract §3.5):
     * If true, cancellation refund amount must be Rs 0.
     *
     * @param policyId the primary key of the policy
     * @return true if any claim associated with policyId has status APPROVED or SETTLED
     */
    boolean hasPaidClaim(Long policyId);

    /**
     * Files a new claim against an active policy.
     * Enforces duplicate detection (same policyId + incidentDate + amount within 24h).
     */
    ClaimResponse fileClaim(ClaimSubmissionRequest request, CurrentUser currentUser);

    /**
     * Retrieves full claim details, enforcing customer or insurer scoping.
     */
    ClaimView getClaim(Long claimId, CurrentUser currentUser);

    /**
     * Lists claims belonging to a specific policy (for customer policy portal).
     */
    List<ClaimView> listClaimsForPolicy(Long policyId, CurrentUser currentUser);

    /**
     * Lists claims for insurer staff with optional status filter.
     */
    List<ClaimView> listClaimsForInsurer(Long insurerId, ClaimStatus status, CurrentUser currentUser);

    /**
     * Claims Officer verifies documents and advances claim from SUBMITTED to ASSESSMENT or DOCUMENTS_REQUIRED.
     */
    ClaimResponse verifyDocuments(Long claimId, ClaimVerificationRequest request, CurrentUser currentUser);

    /**
     * Claims Officer approves or rejects the claim after assessment.
     */
    ClaimResponse recordDecision(Long claimId, ClaimDecisionRequest request, CurrentUser currentUser);

    /**
     * Initiates settlement payout through IPaymentService for an APPROVED claim.
     */
    ClaimResponse processPayout(Long claimId, CurrentUser currentUser);

    /**
     * Scheduled job: closes claims pending documents after 30 days of inactivity.
     */
    int autoCloseStaleClaims();
}
```

### 5.2 Data Transfer Objects and Enums

```java
public enum ClaimStatus {
    SUBMITTED,
    DOCUMENTS_REQUIRED,
    UNDER_ASSESSMENT,
    APPROVED,
    PAYMENT_PROCESSING,
    SETTLED,
    REJECTED,
    CLOSED_STALE
}

public record ClaimSubmissionRequest(
    Long policyId,
    LocalDate incidentDate,
    BigDecimal claimAmount,
    String reason,
    List<Long> documentFileIds
) { }

public record ClaimVerificationRequest(
    boolean documentsComplete,
    List<String> missingDocumentDescriptions,
    String notes
) { }

public record ClaimDecisionRequest(
    String decision,             // "APPROVE" or "REJECT"
    BigDecimal assessedAmount,   // required if decision == "APPROVE"
    String rejectionReason       // required if decision == "REJECT"
) { }

public record ClaimResponse(
    Long claimId,
    String claimNo,              // e.g. "CLM-2026-000101"
    Long policyId,
    ClaimStatus status,
    BigDecimal claimAmount,
    BigDecimal assessedAmount,
    String message
) { }

public record ClaimView(
    Long id,
    String claimNo,
    Long policyId,
    String policyNo,
    Long customerId,
    Long insurerId,
    LocalDate incidentDate,
    BigDecimal claimAmount,
    BigDecimal assessedAmount,
    ClaimStatus status,
    String reason,
    String rejectionReason,
    List<Long> documentFileIds,
    Instant createdAt,
    Instant updatedAt
) { }
```

### 5.3 Claims State Machine (ADR-008)

| Current State | Event / Trigger | Target State | Actor / Guard | Notification Sent |
|---|---|---|---|---|
| `(new)` | Customer submits claim | `SUBMITTED` | Role: `CUSTOMER`. Guard: Policy is `ACTIVE`, incident date within policy term, no duplicate. | `CLAIM_SUBMITTED` |
| `SUBMITTED` | Officer requests missing docs | `DOCUMENTS_REQUIRED` | Role: `CLAIMS_OFFICER`. Documents incomplete. | `CLAIM_DOCUMENTS_REQUIRED` |
| `DOCUMENTS_REQUIRED` | Customer re-uploads documents | `SUBMITTED` | Role: `CUSTOMER`. Document files attached. | `CLAIM_SUBMITTED` |
| `DOCUMENTS_REQUIRED` | Inactivity > 30 days | `CLOSED_STALE` | System scheduler (`autoCloseStaleClaims`). | `CLAIM_CLOSED_STALE` |
| `SUBMITTED` | Officer verifies documents | `UNDER_ASSESSMENT` | Role: `CLAIMS_OFFICER`. Documents verified. | *(None / internal)* |
| `UNDER_ASSESSMENT` | Officer approves claim | `APPROVED` | Role: `CLAIMS_OFFICER`. `assessedAmount <= sumInsured`. | `CLAIM_APPROVED` |
| `UNDER_ASSESSMENT` | Officer rejects claim | `REJECTED` | Role: `CLAIMS_OFFICER`. Rejection reason mandatory. | `CLAIM_REJECTED` |
| `APPROVED` | Officer/System triggers payout | `PAYMENT_PROCESSING` | System / Officer. Calls `IPaymentService.payout()`. | *(None / pending)* |
| `PAYMENT_PROCESSING`| Payout returns `SUCCESS` | `SETTLED` | System payment listener. | `CLAIM_SETTLED` |
| `PAYMENT_PROCESSING`| Payout returns `FAILED` | `APPROVED` | System payment listener (retryable). | `PAYMENT_FAILED` |

*Terminal States:* `SETTLED`, `REJECTED`, `CLOSED_STALE`. No further transitions permitted from terminal states.

### 5.4 Cross-Module Dependencies

- **`IPolicyQuery` (Quote & Purchase C5/C7):** Validates policy exists, status is `ACTIVE`, incident date is within `startDate` and `endDate`, and extracts `insurerId` and `sumInsured`.
- **`IFileStore` (Component C12):** Stores damage bills, FIR copies, diagnostic reports uploaded during filing or re-verification.
- **`IPaymentService` (Component C8):** Disburses approved funds upon settlement.
- **`INotify` (Component C9):** Emits notifications to customers on state transitions.
- **`IAuditLog` (Component C11):** Writes immutable audit record on verification and decision steps.

---

## 6. Section D — File Storage Contract (`IFileStore`)

Component C12 handles binary document storage (KYC proofs, policy PDF certificates, claim receipts) adhering strictly to INS-SR-004 (Encryption at rest) and INS-SR-007 (Secure upload controls).

### 6.1 Java Interface Definition

```java
package com.team11.insurance.filestore;

import com.team11.insurance.auth.CurrentUser;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

/**
 * File Store Interface (Component C12 - SAD Section 3.4, INS-SR-004, INS-SR-007).
 * Consumed by: KYC Intake (C3), Policy Issuance (C7), Claims (C7/C10).
 */
public interface IFileStore {

    /**
     * Stores a multipart file uploaded through REST controllers.
     * Enforces file size (<= 5MB), MIME allowlist, magic-byte inspection, and virus scanning.
     *
     * @param file        the incoming multipart file
     * @param category    storage category (KYC, POLICY, CLAIM)
     * @param currentUser authenticated uploader for ownership tagging
     * @return StoredDocument metadata containing the generated primary key and storage key
     */
    StoredDocument storeFile(MultipartFile file, DocumentCategory category, CurrentUser currentUser);

    /**
     * Stores an internally generated document (e.g. Policy PDF certificate from C7).
     *
     * @param content          input stream of the file content
     * @param originalFilename logical filename for downloads (e.g. "Policy-POL-2026-0001.pdf")
     * @param contentType      MIME type (must be in allowlist)
     * @param sizeBytes        byte count
     * @param category         storage category
     * @param ownerUserId      user ID attributed as document owner
     * @return StoredDocument metadata
     */
    StoredDocument storeStream(InputStream content, String originalFilename, String contentType,
                               long sizeBytes, DocumentCategory category, Long ownerUserId);

    /**
     * Retrieves file content for downloading or streaming, enforcing access permissions.
     *
     * @param documentId  the database ID of the document (matches Policy.documentFileId)
     * @param currentUser requesting user (must be document owner or authorized staff)
     * @return LoadedDocument containing Spring Resource and metadata
     */
    LoadedDocument loadFile(Long documentId, CurrentUser currentUser);

    /**
     * Deletes a document by ID.
     */
    void deleteFile(Long documentId, CurrentUser currentUser);

    /**
     * Checks if a document exists.
     */
    boolean exists(Long documentId);
}
```

### 6.2 File Metadata Models

```java
public enum DocumentCategory {
    KYC,
    POLICY,
    CLAIM
}

public record StoredDocument(
    Long id,                       // Primary Key (Long) - stored in Policy.documentFileId
    String storageKey,             // Random UUID filename (e.g. "a1b2c3d4-e5f6-7890.pdf")
    String originalFilename,       // User-provided display filename
    String contentType,            // "application/pdf", "image/jpeg", "image/png"
    long sizeBytes,                // <= 5 * 1024 * 1024
    DocumentCategory category,
    String sha256Checksum,         // Integrity check
    Long ownerUserId,
    Instant uploadedAt
) { }

public record LoadedDocument(
    StoredDocument metadata,
    Resource resource
) { }
```

### 6.3 Security Controls and Storage Configuration

1. **Storage Root Configuration:**
   - Property: `insurance.filestore.root-dir=${FILE_STORE_DIR:./storage/uploads}`
   - Directory hierarchy:
     ```text
     storage/uploads/
     ├── kyc/
     ├── policies/
     └── claims/
     ```
2. **Path Traversal Prevention:**
   - **Never use client-supplied filenames on disk.**
   - Physical filename is strictly a generated UUID + validated extension: `UUID.randomUUID().toString() + extension`.
   - Path verification: Target path must be canonicalized and checked via `resolvedPath.normalize().startsWith(rootDir.normalize())`. If false, throw `SecurityException`.
3. **MIME Allowlist & Magic-Byte Validation (INS-SR-007):**
   - Strictly permitted types: `application/pdf`, `image/jpeg`, `image/png`.
   - File extensions permitted: `.pdf`, `.jpg`, `.jpeg`, `.png`.
   - Validation inspects actual header magic bytes (e.g. `%PDF-` for PDF, `FF D8 FF` for JPEG, `89 50 4E 47` for PNG) using Apache Tika or standard byte inspection. Disallow executable/script uploads.
4. **File Size Limit (INS-SR-007):**
   - Maximum file size: **5,242,880 bytes (5 MB)**. Exceeding requests throw `VALID-422` with message `File size exceeds maximum limit of 5MB`.
5. **Simulated Virus Scanning:**
   - Integrates `IVirusScanner` abstraction. In test/dev profile, validates file does not contain EICAR standard test signature. Rejects infected files with `VALID-422` and logs a security warning.
6. **Encryption at Rest (INS-SR-004):**
   - Documents are stored in encrypted volumes (AES-256) or encrypted with AES/GCM before writing to the block device.

---

## 7. Section E — Injectable Clock and Test Clock Endpoint

To support time-dependent workflow verification (e.g. 15-day free-look cancellation refund, 30/15/1-day renewal reminders, policy expiry, and 30-day stale claim closure), the system requires an injectable `Clock` abstraction and an isolated test-only control endpoint.

### 7.1 Clock Abstraction Design

- **Core Bean:** Standard `java.time.Clock` registered as a Spring bean.
- **Production Profile (`@Profile("prod")`):**
  ```java
  @Bean
  @Profile("prod")
  public Clock productionClock() {
      return Clock.systemDefaultZone();
  }
  ```
- **Development / Test Profiles (`@Profile({"dev", "test"})`):**
  Uses `MutableTestClock` wrapping an internal `Clock` with support for time-travel offsets and fixed instants.

```java
package com.team11.insurance.common.time;

import java.time.*;

public class MutableTestClock extends Clock {
    private Clock delegate = Clock.systemDefaultZone();
    private Duration offset = Duration.ZERO;

    public synchronized void setFixed(Instant fixedInstant, ZoneId zone) {
        this.delegate = Clock.fixed(fixedInstant, zone);
        this.offset = Duration.ZERO;
    }

    public synchronized void advance(Duration duration) {
        this.offset = this.offset.plus(duration);
    }

    public synchronized void reset() {
        this.delegate = Clock.systemDefaultZone();
        this.offset = Duration.ZERO;
    }

    @Override
    public ZoneId getZone() { return delegate.getZone(); }

    @Override
    public Clock withZone(ZoneId zone) { return delegate.withZone(zone); }

    @Override
    public Instant instant() {
        return delegate.instant().plus(offset);
    }
}
```

### 7.2 Test-Only Clock Control Endpoint

- **Route:** `POST /api/v1/test/clock`
- **Inspection Route:** `GET /api/v1/test/clock`
- **Profiles:** `@Profile({"dev", "test"})` only.

#### Request Payloads

1. **Advance Time (relative offset):**
   ```json
   {
     "action": "ADVANCE",
     "amount": 16,
     "unit": "DAYS"
   }
   ```
2. **Set Fixed Instant:**
   ```json
   {
     "action": "SET",
     "isoInstant": "2026-11-01T12:00:00Z"
   }
   ```
3. **Reset to System Time:**
   ```json
   {
     "action": "RESET"
   }
   ```

#### Response Payload (`200 OK`)

```json
{
  "status": "SUCCESS",
  "action": "ADVANCE",
  "virtualInstant": "2026-11-02T10:00:00Z",
  "virtualDate": "2026-11-02",
  "offsetSeconds": 1382400
}
```

### 7.3 Production Safeguards and Profile Isolation

1. **Strict Spring Profile Guard:**
   - The controller class `TestClockController` and configuration `TestClockConfig` are annotated with `@Profile({"dev", "test"})`.
2. **Startup Profile Validation (matching `ProductionProfileIsolationTest`):**
   - During bean initialization (`@PostConstruct`), `TestClockController` verifies active profiles against an environment check:
     ```java
     for (String profile : environment.getActiveProfiles()) {
         if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
             throw new IllegalStateException("FATAL: TestClockController cannot be enabled in production profile!");
         }
     }
     ```
3. **HTTP 404 in Production:**
   - In production deployments, Spring never registers routes under `/api/v1/test/*`. Any request receives HTTP `404 Not Found`.

---

## 8. Team Decision Register & Sign-Off Matrix

The following table summarizes the proposed contract options, alternatives, compatibility implications, and their status for the team sign-off meeting:

| # | Decision Topic | Proposed Option | Alternative Option(s) | Compatibility Implications | Team Approval Status |
|---|---|---|---|---|---|
| **D-09.1** | Payment Result & Error Mapping | Return `PaymentResult` with `SUCCESS` / `FAILED` status, throwing `PaymentInfrastructuralException` only on dead network. | Throw checked exceptions on payment decline. | Compatible with P3 Contract §3.2 (`402 PAY-402` mapped from `FAILED`). | **Pending Team Sign-off** |
| **D-09.2** | Payment Idempotency Key Format | Server mints `applicationId + "-" + attemptNo`. Client never passes key. | Client generates random UUID in header. | Avoids replay risks; matches P3 Contract §3.2. | **Pending Team Sign-off** |
| **D-09.3** | Zero-Refund Policy Cancellation | If `hasPaidClaim` is true, refund amount = Rs 0 and gateway is skipped. | Make Rs 0 refund call to payment gateway. | Avoids gateway error on zero amount; matches P3 §3.5. | **Pending Team Sign-off** |
| **D-09.4** | Notification Delivery Pattern | Persist outbox table + `@Async` fire-and-forget; transient failures retry 3x; non-blocking to business callers. | Synchronous delivery during HTTP request; message broker (RabbitMQ/Kafka). | Ensures zero transaction rollbacks from notification failure; minimal dependency footprint. | **Pending Team Sign-off** |
| **D-09.5** | `IClaimService.hasPaidClaim(policyId)` Semantics | Returns true if any claim on policy is in status `APPROVED` or `SETTLED`. | Check only `SETTLED` claims. | Directly fulfills P3 Contract §3.5 requirement for cancellation refund oracle. | **Pending Team Sign-off** |
| **D-09.6** | Stale Claim Inactivity Threshold | Automatically transition claims in `DOCUMENTS_REQUIRED` to `CLOSED_STALE` after 30 days of inactivity. | Never close claims; manually close by Claims Officer. | Fulfills ADR-008 and automated housekeeping requirements. | **Pending Team Sign-off** |
| **D-09.7** | File Storage Primary Key & Keying | Database primary key `Long id` for relational entity linking (`Policy.documentFileId`), physical disk file named via UUID. | String UUID as database primary key. | Fully compatible with existing `Policy.java` entity line 81. | **Pending Team Sign-off** |
| **D-09.8** | File Storage Security Constraints | Max 5 MB, magic-byte inspection (PDF/JPG/PNG), canonical path traversal check, simulated virus scan. | Extension-only check, arbitrary file sizes. | Mandatory compliance with INS-SR-004 and INS-SR-007. | **Pending Team Sign-off** |
| **D-09.9** | Clock Abstraction & Profile Isolation | Standard `java.time.Clock` bean with `MutableTestClock` in `dev`/`test` and `POST /api/v1/test/clock`; fail startup in `prod`. | Custom `TimeService` interface; manual mock injection in every test. | Non-invasive to Spring ecosystem; matches P2-02 `ProductionProfileIsolationTest` pattern. | **Pending Team Sign-off** |

---

## 9. Open Questions for Team Sign-Off Meeting

The following specific questions must be answered during the upcoming contract review meeting before marking P4-03 approved:

- [ ] **Q1 (Payments):** Does the team approve the `PaymentResult` record structure and the server-generated `applicationId + "-" + attemptNo` idempotency format?
- [ ] **Q2 (Payments):** Does Person 3 agree that `refund(policyId, 0, ...)` should immediately return `SUCCESS` without attempting a gateway call?
- [ ] **Q3 (Notifications):** Is the proposed `NotificationType` enum list comprehensive for all sprint requirements, or are additional event names needed?
- [ ] **Q4 (Claims):** Does Person 1 agree with the Claims Officer decision payload structure (`decision: "Approve" | "Reject"`, `assessedAmount`, `rejectionReason`) matching API Contract 01 §6.2?
- [ ] **Q5 (Claims):** Should `hasPaidClaim(policyId)` treat `APPROVED` claims (prior to payout) as paid for the purpose of policy cancellation refunds, or strictly `SETTLED`?
- [ ] **Q6 (File Store):** Does the team approve using `Long id` as the primary key for documents to maintain backwards compatibility with `Policy.documentFileId`?
- [ ] **Q7 (Clock):** Does Person 2 and Person 3 agree on the payload shape for `POST /api/v1/test/clock` (`ADVANCE`, `SET`, `RESET`) for end-to-end Newman/Selenium tests?

---

## 10. Review and Approval Register

- **Person 1 — QA Lead:** Thijil Thejaswi M N (`Thijil-Thejaswi-MN`) — *Pending Review*
- **Person 2 — Functional Test Engineer:** Sharana (`sharana-k`) — *Pending Review*
- **Person 3 — Non-functional & Security Test Engineer:** Samarth (`samarth-b-m`) — *Pending Review*
- **Person 4 — Developer / Environment Owner:** Raghunandan Kotary (`Raghunandan-Kotary`) — *Draft Prepared*

*Note: P4-05 (Test Infrastructure) and P4-06 (Interface Stubs) remain BLOCKED until explicit approval from all team members is recorded.*
