# Non-functional and security tests (P3-03)

**Owner:** Person 3 (Samarth), Non-functional and Security Test Engineer
**Status:** Prepared skeletons. **Nothing here has been executed.** Every result stays "Not Run" until a real run produces evidence (CONTRIBUTING section 8).
**Source:** STP Appendix A (TC-Perf-01, TC-Load-01, TC-Sec-01 to TC-Sec-11), STP section 6 (test data, environment), `docs/api-contract/`.

## Contents

| File | Purpose |
| --- | --- |
| `jmeter/TC-Perf-01-quote-response-time.jmx` | 50 users, ramp 1 min, hold 10 min, motor and health quotes. Pass: p95 <= 5 s, errors < 1% |
| `jmeter/TC-Load-01-500-concurrent-users.jmx` | 500 users, ramp 5 min, hold 15 min, mix 40/30/10/10/10. Pass: errors < 1%, quote p95 <= 5 s, no crash or connection exhaustion |
| `jmeter/users.csv` | Sample test users. Replace with the loader's KYC-verified users (one row per virtual user, or at least enough to avoid shared-session effects) |
| `pen-test-checklist.md` | Token tampering, insurerId tampering, double payment, replay |
| `environment-needs.md` | What Person 4 must provide for these tests |

## Before the first real run (open items)

- Open both `.jmx` files in the JMeter GUI once and check they load. They were generated and checked as well-formed XML only, not opened in JMeter.
- Confirm the login response (`accessToken`) and the plan ids (`motorPlanId`, `healthPlanId`; the seeder currently gives PLAN-A-M1 = 1 and PLAN-A-H1 = 3).
- TC-Load-01 placeholders: claim-status path and dashboard path (`-JclaimStatusPath`, `-JdashboardPath`), and the application step needs KYC-verified users. Confirm with the owners once implemented.
- The "mix" uses percentage controllers, so the split is approximate over many iterations.
- Run the load generator on a **second machine**, not the one hosting the app (STP section 6).

## Run commands (JMeter, headless)

```powershell
cd docs\nf-security-tests\jmeter
# TC-Perf-01
jmeter -n -t TC-Perf-01-quote-response-time.jmx -Jhost=<app-host> -Jport=<port> -Jprotocol=https `
  -JusersFile=users.csv -l perf01.jtl -e -o perf01-report
# TC-Load-01
jmeter -n -t TC-Load-01-500-concurrent-users.jmx -Jhost=<app-host> -Jport=<port> -Jprotocol=https `
  -JusersFile=users.csv -l load01.jtl -e -o load01-report
```
Override size or timing with `-Jusers=`, `-Jrampup=`, `-Jduration=` (duration includes the ramp-up) for a short dry run, for example `-Jusers=5 -Jrampup=5 -Jduration=30`.
Do not commit `.jtl` files or report folders larger than a few MB; attach summaries to the test issue instead.

## ZAP baseline (TC-Sec-06)

Needs Docker and a running application.

```powershell
docker run --rm -v "${PWD}:/zap/wrk:rw" -t ghcr.io/zaproxy/zaproxy:stable `
  zap-baseline.py -t http://host.docker.internal:8080 -r zap-baseline.html -J zap-baseline.json
```
The baseline is a passive scan. It does not replace the manual SQL injection and XSS strings listed in TC-Sec-06. Check the image name against the ZAP documentation before use.

## TLS scan (TC-Sec-01)

Needs the Nginx TLS environment from Person 4. The local dev setup is plain HTTP and cannot pass this test.

```bash
# Linux, macOS or WSL
./testssl.sh --protocols --headers https://<test-host>
# or with Docker
docker run --rm -ti drwetter/testssl.sh --protocols --headers https://<test-host>
```
Expected (STP): HTTP redirected or refused, only TLS 1.2 and 1.3 offered, TLS 1.0 and 1.1 refused, HSTS present.

## Evidence rules

Record date, build or commit, environment and the result files for every run. Report failures as defects. Do not mark a test passed from a skeleton or a partial run.
