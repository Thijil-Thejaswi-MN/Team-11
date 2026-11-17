# Sprint 1 demo video (Team11_Sprint1_Demo.mp4)
Recorded 27 Oct 2026 (after the 23 Oct Sprint 1 deadline) from the main branch, dev profile.
The exact build commit was not recorded at the time of recording.

What the video shows: a walkthrough of the Sprint 1 screens per role.
Important: several screens still display built-in SAMPLE data, not live results:
- Insurer Admin metrics (1,420 policies, ₹1.18 Cr, 142 claims, 62.4%)
- Application APP-2026-9021 timeline, dashboard policies, claim CLM-2026-0042, audit preview
The quote screens are shown before calculation (no premium is computed in the video).

Working on main (backend, verified by automated tests): plans browse/compare API,
motor/health quote engines, underwriting, application submission, simulated payment,
policy issuance + PDF.
Not yet working at the time of recording: real login screen (the role selector was a dev stub),
KYC upload, live data on admin/claims screens. Real login (PR #162) and KYC upload (PR #161) were
merged later on 27 Oct and will be shown in the Sprint 2 video.

---

# Sprint 2 demo video (Team11_Sprint2_Demo.mp4)
Recorded 17 Nov 2026 from the integrated build (`dev` profile, connected to live MySQL 8 database).

What the video demonstrates:
- **Authentication & User Management:** User registration, real session login, and role-based portal access.
- **KYC Verification:** Customer self-service KYC submission with government-issued PAN/Aadhaar details and instant automated verification approval.
- **Straight-Through Processing (STP) Quoting:** Instant premium calculation for motor and health policies based on rating formulas.
- **Application & Underwriting:** Application submission, automated underwriting rules evaluation, and underwriter review desk.
- **Policy Issuance & Downloads:** Secure payment processing and certified policy schedule PDF generation.
- **Claims Workflow:** Claim filing with supporting documentation and claims officer assessment desk.
- **Governance & Audit Trail:** Multi-tenant plan management, insurer onboarding, and immutable compliance audit logging.
