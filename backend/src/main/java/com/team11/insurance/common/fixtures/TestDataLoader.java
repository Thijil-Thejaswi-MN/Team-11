package com.team11.insurance.common.fixtures;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.admin.repository.InsurerRepository;
import com.team11.insurance.auth.repository.UserRepository;
import com.team11.insurance.plans.PlanRepository;
import com.team11.insurance.policies.PolicyRepository;

import jakarta.persistence.EntityManager;

/**
 * Deterministic loader for the canonical STP Section 6 & Appendix A test fixtures.
 * <p>
 * Work Item: P4-05
 * Owner: Person 4 (Raghunandan Kotary)
 * <p>
 * Loads fixtures in strict foreign-key dependency order:
 * <ol>
 *   <li>Insurers (insurers.json)</li>
 *   <li>Users (users.json)</li>
 *   <li>Plans (plans.json)</li>
 *   <li>Policies (policies.json)</li>
 *   <li>Claims (claims.json - loaded dynamically if table exists)</li>
 * </ol>
 * Uses {@link JdbcTemplate} and {@link TransactionTemplate} with PROPAGATION_REQUIRES_NEW
 * to guarantee atomic, all-or-nothing execution that rolls back on any insertion failure.
 * Checks per-record existence for deterministic idempotency rather than naive table row counts.
 * Restricted strictly to 'dev' and 'test' profiles.
 */
@Component
@Profile({"dev", "test"})
public class TestDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDataLoader.class);

    private static final String DEFAULT_PASSWORD_HASH =
            "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewfY5v7m2G9J7Lce"; // Password123!

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final InsurerRepository insurerRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PolicyRepository policyRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    public TestDataLoader(JdbcTemplate jdbcTemplate,
                          PlatformTransactionManager transactionManager,
                          InsurerRepository insurerRepository,
                          UserRepository userRepository,
                          PlanRepository planRepository,
                          PolicyRepository policyRepository,
                          EntityManager entityManager,
                          ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.insurerRepository = insurerRepository;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.policyRepository = policyRepository;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        log.info("TestDataLoader: Checking canonical test fixtures initialization...");
        loadCanonicalFixtures(false);
    }

    /**
     * Loads the canonical fixtures in foreign-key dependency order within an atomic transaction.
     * If any fixture insert fails, the entire transaction is rolled back and the exception is propagated.
     *
     * @param resetExisting if true, clears and reloads the fixture tables
     */
    public synchronized void loadCanonicalFixtures(boolean resetExisting) {
        transactionTemplate.execute(status -> {
            try {
                if (resetExisting) {
                    log.warn("TestDataLoader: Clearing existing fixture records in reverse FK order...");
                    if (tableExists("claims")) {
                        jdbcTemplate.execute("DELETE FROM claims");
                    }
                    jdbcTemplate.execute("DELETE FROM policies");
                    jdbcTemplate.execute("DELETE FROM plans");
                    jdbcTemplate.execute("DELETE FROM users");
                    jdbcTemplate.execute("DELETE FROM insurers");
                    entityManager.flush();
                    entityManager.clear();
                }

                loadInsurers();
                loadUsers();
                loadPlans();
                loadPolicies();
                loadClaims();
                entityManager.clear();
                log.info("TestDataLoader: Canonical test fixture loading complete.");
                return null;
            } catch (Exception e) {
                log.error("TestDataLoader: Error during fixture loading, rolling back transaction: {}", e.getMessage());
                status.setRollbackOnly();
                if (e instanceof RuntimeException re) {
                    throw re;
                }
                throw new IllegalStateException("Failed to load canonical test fixtures: " + e.getMessage(), e);
            }
        });
    }

    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE UPPER(table_name) = UPPER(?)",
                    Integer.class, tableName
            );
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void loadInsurers() throws Exception {
        ClassPathResource resource = new ClassPathResource("fixtures/insurers.json");
        try (InputStream is = resource.getInputStream()) {
            List<Map<String, Object>> list = objectMapper.readValue(is, new TypeReference<>() {});
            for (Map<String, Object> map : list) {
                Long id = ((Number) map.get("id")).longValue();
                Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM insurers WHERE id = ?", Integer.class, id);
                if (count == null || count == 0) {
                    String name = (String) map.get("name");
                    String status = (String) map.get("status");
                    boolean active = "ACTIVE".equalsIgnoreCase(status);

                    jdbcTemplate.update(
                            "INSERT INTO insurers (id, name, active, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                            id, name, active
                    );
                }
            }
            log.info("TestDataLoader: Insurers fixture verified/loaded.");
        }
    }

    private void loadUsers() throws Exception {
        ClassPathResource resource = new ClassPathResource("fixtures/users.json");
        try (InputStream is = resource.getInputStream()) {
            List<Map<String, Object>> list = objectMapper.readValue(is, new TypeReference<>() {});
            for (Map<String, Object> map : list) {
                Long id = ((Number) map.get("id")).longValue();
                String email = (String) map.get("email");
                Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE id = ? OR email = ?", Integer.class, id, email);
                if (count == null || count == 0) {
                    String name = (String) map.get("name");
                    String roleStr = (String) map.get("role");
                    Long insurerId = map.get("insurerId") != null ? ((Number) map.get("insurerId")).longValue() : null;

                    jdbcTemplate.update(
                            "INSERT INTO users (id, name, email, phone, password_hash, role, insurer_id, failed_login_attempts, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                            id, name, email, "+919876543210", DEFAULT_PASSWORD_HASH, roleStr, insurerId
                    );
                }
            }
            log.info("TestDataLoader: Users fixture verified/loaded.");
        }
    }

    private void loadPlans() throws Exception {
        ClassPathResource resource = new ClassPathResource("fixtures/plans.json");
        try (InputStream is = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            for (JsonNode node : root) {
                Long id = node.get("id").asLong();
                String code = node.get("code").asText();
                Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM plans WHERE id = ? OR code = ?", Integer.class, id, code);
                if (count == null || count == 0) {
                    Long insurerId = node.get("insurerId").asLong();
                    String type = node.get("type").asText();
                    String name = node.get("name").asText();
                    BigDecimal coverage = node.get("coverage").decimalValue();
                    BigDecimal deductible = node.get("deductible").decimalValue();
                    int waiting = node.get("waitingPeriodMonths").asInt();
                    BigDecimal basePremium = node.get("basePremium").decimalValue();
                    String premiumRulesJson = node.has("premiumRules") && !node.get("premiumRules").isNull()
                            ? node.get("premiumRules").toString() : null;
                    String uwRulesJson = node.has("uwRules") && !node.get("uwRules").isNull()
                            ? node.get("uwRules").toString() : null;
                    boolean active = node.get("active").asBoolean();

                    jdbcTemplate.update(
                            "INSERT INTO plans (id, code, insurer_id, type, name, coverage, deductible, waiting_period_months, base_premium, premium_rules_json, underwriting_rules_json, add_ons_json, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '[]', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                            id, code, insurerId, type, name, coverage, deductible, waiting, basePremium, premiumRulesJson, uwRulesJson, active
                    );
                }
            }
            log.info("TestDataLoader: Plans fixture verified/loaded.");
        }
    }

    private void loadPolicies() throws Exception {
        ClassPathResource resource = new ClassPathResource("fixtures/policies.json");
        try (InputStream is = resource.getInputStream()) {
            List<Map<String, Object>> list = objectMapper.readValue(is, new TypeReference<>() {});
            for (Map<String, Object> map : list) {
                Long id = ((Number) map.get("id")).longValue();
                String policyNo = (String) map.get("policyNo");
                Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM policies WHERE id = ? OR policy_no = ?", Integer.class, id, policyNo);
                if (count == null || count == 0) {
                    Long customerId = ((Number) map.get("customerId")).longValue();
                    Long insurerId = ((Number) map.get("insurerId")).longValue();
                    Long planId = ((Number) map.get("planId")).longValue();
                    BigDecimal premium = BigDecimal.valueOf(((Number) map.get("premium")).doubleValue());
                    LocalDate startDate = LocalDate.parse((String) map.get("startDate"));
                    LocalDate endDate = LocalDate.parse((String) map.get("endDate"));
                    String vehicleRegNo = (String) map.get("vehicleRegNo");
                    String statusStr = (String) map.get("status");

                    jdbcTemplate.update(
                            "INSERT INTO policies (id, policy_no, customer_id, insurer_id, plan_id, premium_paid, start_date, end_date, vehicle_reg_no, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                            id, policyNo, customerId, insurerId, planId, premium, Date.valueOf(startDate), Date.valueOf(endDate), vehicleRegNo, statusStr
                    );
                }
            }
            log.info("TestDataLoader: Policies fixture verified/loaded.");
        }
    }

    private void loadClaims() throws Exception {
        if (!tableExists("claims")) {
            log.info("TestDataLoader: Table 'claims' does not exist yet (owned by P3/C7), skipping claims fixture.");
            return;
        }
        ClassPathResource resource = new ClassPathResource("fixtures/claims.json");
        try (InputStream is = resource.getInputStream()) {
            List<Map<String, Object>> list = objectMapper.readValue(is, new TypeReference<>() {});
            for (Map<String, Object> map : list) {
                Long id = ((Number) map.get("id")).longValue();
                String claimNo = (String) map.get("claimNo");
                Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM claims WHERE id = ? OR claim_no = ?", Integer.class, id, claimNo);
                if (count == null || count == 0) {
                    Long policyId = ((Number) map.get("policyId")).longValue();
                    String policyNo = (String) map.get("policyNo");
                    Long customerId = ((Number) map.get("customerId")).longValue();
                    Long insurerId = ((Number) map.get("insurerId")).longValue();
                    LocalDate incidentDate = LocalDate.parse((String) map.get("incidentDate"));
                    BigDecimal claimAmount = BigDecimal.valueOf(((Number) map.get("claimAmount")).doubleValue());
                    BigDecimal settledAmount = map.get("settledAmount") != null
                            ? BigDecimal.valueOf(((Number) map.get("settledAmount")).doubleValue()) : null;
                    String statusStr = (String) map.get("status");
                    String reason = (String) map.get("reason");

                    jdbcTemplate.update(
                            "INSERT INTO claims (id, claim_no, policy_id, policy_no, customer_id, insurer_id, incident_date, claim_amount, settled_amount, status, reason, duplicate_flag, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                            id, claimNo, policyId, policyNo, customerId, insurerId, Date.valueOf(incidentDate), claimAmount, settledAmount, statusStr, reason
                    );
                }
            }
            log.info("TestDataLoader: Claims fixture verified/loaded.");
        }
    }
}
