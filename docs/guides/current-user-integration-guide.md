# CurrentUser Identity & Test Stub Integration Guide

**Work Item:** P2-02  
**Owner:** Person 2 — Sharana (Functional Test Engineer)  
**Target Audience:** Person 1 (Quotes/Underwriting), Person 3 (Applications/Policies), Person 4 (Backend Skeleton/Controllers)  
**Date:** 16 October 2026 (Day 4 Checkpoint)  
**Contract Reference:** API Contract Draft 02 (§6), SAD §4.3.1, §4.3.7, SRS `INS-SR-003`

---

## 1. Overview

The `CurrentUser` context represents the authenticated identity in the Insurance Workflow Automation System.

```json
{
  "userId": 1001,
  "role": "CUSTOMER",
  "insurerId": null
}
```

### Roles Supported:
- `CUSTOMER` — Policyholders (insurerId is `null`)
- `INSURER_ADMIN` — Insurer administrator (requires valid `insurerId`)
- `UNDERWRITER` — Underwriting specialist (requires valid `insurerId`)
- `CLAIMS_OFFICER` — Claims handler (requires valid `insurerId`)
- `SYSTEM_ADMIN` — Global platform administrator (insurerId is `null`)

---

## 2. Using `CurrentUser` in Controller Endpoints

Spring MVC controllers written by any team member can inject `CurrentUser` directly into controller handler methods.

### Option A: Method Parameter Injection (Recommended)

```java
package com.team11.insurance.applications;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.annotation.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    @GetMapping
    public ResponseEntity<?> listApplications(@CurrentUser CurrentUser user) {
        // user.getUserId() -> 1001
        // user.getRole() -> "CUSTOMER"
        // user.isCustomer() -> true
        return ResponseEntity.ok(...);
    }
}
```

### Option B: Service Injection via `ICurrentUserProvider`

```java
package com.team11.insurance.policies;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.ICurrentUserProvider;
import org.springframework.stereotype.Service;

@Service
public class PolicyService {

    private final ICurrentUserProvider currentUserProvider;

    public PolicyService(ICurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    public void processPolicy() {
        CurrentUser user = currentUserProvider.getCurrentUser();
        Long actorId = user.getUserId();
        // ...
    }
}
```

---

## 3. Enforcing Role & Insurer Isolation (INS-SR-003)

Use `RoleSecurity` to enforce access control and tenant isolation. It throws `AccessDeniedException` (`ACCESS-403`) upon violation.

### Role Check
```java
// Throws AccessDeniedException (403) if caller is not an Underwriter or Insurer Admin
RoleSecurity.assertRole(user, UserRole.UNDERWRITER, UserRole.INSURER_ADMIN);
```

### Insurer Isolation Check
```java
// Staff members can only view/manage records belonging to their own insurer
RoleSecurity.assertInsurerAccess(user, application.getInsurerId());
```

### Customer Data Isolation Check
```java
// Customers can only view their own records; staff/admin can view any customer's records
RoleSecurity.assertCustomerOrStaffAccess(user, targetCustomerId);
```

---

## 4. Testing with Synthetic Identities (P1, P3, P4)

In unit and integration tests under the `test` or `dev` Spring profiles, `TestCurrentUserProvider` provides `SEEDED_CUSTOMER` (Asha Verma, `userId: 1001`, `role: "CUSTOMER"`) by default.

To test operations requiring staff or admin roles:

```java
import com.team11.insurance.auth.TestCurrentUserProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class UnderwritingControllerTest {

    @AfterEach
    void tearDown() {
        // Always reset test context after test
        TestCurrentUserProvider.clearSyntheticUser();
    }

    @Test
    void testUnderwriterDecision() {
        // Switch synthetic context to Underwriter Priya (Insurer 1)
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_UNDERWRITER);

        // Perform test calls...
    }

    @Test
    void testSystemAdminReports() {
        // Switch to System Admin
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_SYSTEM_ADMIN);

        // Perform test calls...
    }
}
```

### Predefined Fixture Constants:
- `TestCurrentUserProvider.SEEDED_CUSTOMER` (`1001L`, `"CUSTOMER"`, `null`)
- `TestCurrentUserProvider.SEEDED_INSURER_ADMIN` (`2001L`, `"INSURER_ADMIN"`, `1L`)
- `TestCurrentUserProvider.SEEDED_UNDERWRITER` (`3001L`, `"UNDERWRITER"`, `1L`)
- `TestCurrentUserProvider.SEEDED_CLAIMS_OFFICER` (`4001L`, `"CLAIMS_OFFICER"`, `1L`)
- `TestCurrentUserProvider.SEEDED_SYSTEM_ADMIN` (`5001L`, `"SYSTEM_ADMIN"`, `null`)

---

## 5. Security Safeguards

1. **No Arbitrary Role Spoofing:** Clients cannot send headers (e.g. `X-Role: SYSTEM_ADMIN`) to select their own role. Identity is strictly derived by the server-side provider.
2. **Production Fail-Fast:** `TestCurrentUserProvider` verifies active profiles during `@PostConstruct`. If the active profile is `prod` or `production`, application startup fails immediately with an `IllegalStateException`.
3. **Production Implementation:** Under the `prod` profile, `ProductionCurrentUserProvider` activates and requires a verified server-side session or token; unauthenticated calls are rejected with `AUTH-401`.
