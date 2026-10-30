# User Acceptance Testing (UAT) Execution Sheet — 29 Oct 2026

**Build SHA:** `00add8445c4f0105a20d24c2fc28d3ebd6a32d45`  
**Date:** 29 Oct 2026  
**Roles & Assigned Team Members:**
- **Sharana (Person 2):** Test Execution Lead
- **Thijil (Person 1):** QA Recording Lead & System Admin
- **Samarth (Person 3):** Underwriter & Claims Officer
- **Raghunandan (Person 4):** Customer & Test Environment Lead

---

## STP UAT Scenarios

| ID | Scenario | Roles | Expected | Actual | Pass/Fail/Blocked | Evidence | Defect | Tested by | Time |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **UAT-01** | Customer buys a 4-wheeler policy: browse, filter, quote, apply, auto-approve, pay, download the policy PDF. | Sharana executes; Thijil records + System Admin; Samarth Underwriter/Claims Officer; Raghunandan Customer + environment. | Policy is Active and the PDF is correct. (filter step: INS-F-022 deferred, D-10) | | | | | | |
| **UAT-02** | Customer buys a health policy; the application is referred (age 62); Underwriter approves; customer pays. | Sharana executes; Thijil records + System Admin; Samarth Underwriter/Claims Officer; Raghunandan Customer + environment. | Referred application is decided and the policy issued after payment. | | | | | | |
| **UAT-03** | Customer renews a motor policy within the window and downloads the renewal certificate. | Sharana executes; Thijil records + System Admin; Samarth Underwriter/Claims Officer; Raghunandan Customer + environment. | New policy linked to the old one; premium as per the renewal rules. | | | | | | |
| **UAT-04** | Customer cancels a policy; Insurer Admin approves; refund shown. | Sharana executes; Thijil records + System Admin; Samarth Underwriter/Claims Officer; Raghunandan Customer + environment. | Refund equals the formula in SRS 4.6.2. | | | | | | |
| **UAT-05** | Customer files a motor claim; Claims Officer verifies, assesses, approves; payout settles. | Sharana executes; Thijil records + System Admin; Samarth Underwriter/Claims Officer; Raghunandan Customer + environment. | Claim reaches Settled with notifications at each step. | | | | | | |
| **UAT-06** | A duplicate claim is flagged and reviewed manually; a claim with missing documents is closed after 15 days. | Sharana executes; Thijil records + System Admin; Samarth Underwriter/Claims Officer; Raghunandan Customer + environment. | Flag and Closed - No Response behave as specified. | | | | | | |
| **UAT-07** | Insurer Admin creates and deactivates a plan; staff of another insurer cannot see this insurer's data. | Sharana executes; Thijil records + System Admin; Samarth Underwriter/Claims Officer; Raghunandan Customer + environment. | Plan visible to customers; isolation holds. | | | | | | |
| **UAT-08** | System Admin onboards an insurer and a staff user, generates reports and searches the audit log. | Sharana executes; Thijil records + System Admin; Samarth Underwriter/Claims Officer; Raghunandan Customer + environment. | Reports match data; audit log shows the actions. (reports: INS-F-025 deferred, D-10) | | | | | | |

