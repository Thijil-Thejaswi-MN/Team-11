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
merged later on 27 Oct and will be shown in the Sprint 2 video.
