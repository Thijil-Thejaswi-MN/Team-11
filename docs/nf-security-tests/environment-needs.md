# Environment needs for NF and security tests (P3-03)

For Raghunandan (Person 4, environment owner). Source: STP sections 6 and 9.

| Need | Why | Used by |
| --- | --- | --- |
| **TLS in front of the app**: Nginx reverse proxy with TLS 1.2+ (self-signed is fine), HTTP redirected to HTTPS, HSTS header, TLS 1.0/1.1 off | testssl.sh scan and "all traffic over HTTPS" | TC-Sec-01, TC-Pay-03 |
| **Volume data**: 10,000 plans-and-policies and 5,000 claims, loaded by the test-data loader, plus enough KYC-verified customer users (one per virtual user if possible) | production-like data volume | TC-Perf-01, TC-Load-01 |
| **A second machine for JMeter** (not the app host), reachable from the app host's network; 4 GB or more RAM, Java 17 | 500 virtual users must not compete with the app for CPU | TC-Load-01 |
| **App host sized as in the STP**: 4 vCPU and 8 GB RAM minimum, MySQL 8 (docker-compose), a state of the app that can be reset to the known test data with one command | comparable, repeatable runs | all performance tests |
| **Monitoring access**: CPU, memory, DB connections, error counts (for example actuator metrics, DB status) during runs | TC-Load-01 step 2: no crash or connection exhaustion | TC-Load-01 |
| **Rate limiting**: tell me what limits exist; allow-list the load machine or relax them in the test profile | the skeleton has a RateLimitException; limits would distort results | TC-Perf-01, TC-Load-01 |
| **Controls**: injectable test clock, payment gateway switch (SUCCESS, FAILED, TIMEOUT), PDF-failure switch | session, lockout, replay and double-payment tests | TC-Sec-05, TC-Sec-10, pen-test sections 3 and 4 |
| **Safari access**: a Mac or iPhone, or a cloud browser service. Browsers: latest two versions of Chrome, Firefox, Edge and Safari | browser compatibility (STP section 5) | INS-NF-006 |

## Message to send

> Raghunandan, environment needs for the NF and security tests (details in docs/nf-security-tests/environment-needs.md): (1) Nginx TLS 1.2+ with HTTP redirect and HSTS, (2) the 10,000 plans/policies and 5,000 claims volume data plus KYC-verified load users via the loader, (3) a second machine for JMeter (4 GB+ RAM, Java 17), (4) app host 4 vCPU / 8 GB with MySQL 8, resettable with one command, (5) monitoring access for CPU, memory and DB connections during runs, (6) rate-limit settings or an allow-list for the load machine, (7) clock, payment and PDF-failure switches. Which of these can you provide, and by when?
>
> Team: who has a Mac or iPhone, or can arrange cloud Safari access, for the browser compatibility tests (INS-NF-006)?
