package com.team11.insurance.policies;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationHistory;
import com.team11.insurance.applications.ApplicationHistoryRepository;
import com.team11.insurance.applications.ApplicationRepository;
import com.team11.insurance.applications.ApplicationStatus;

/** Checks that the P3-02 entities create the tables and that the key constraints hold (H2, MySQL mode). */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DataModelTest {

    @Autowired private ApplicationRepository applications;
    @Autowired private ApplicationHistoryRepository history;
    @Autowired private PolicyRepository policies;
    @Autowired private CancellationRequestRepository cancellations;

    private final java.util.concurrent.atomic.AtomicLong quoteSeq = new java.util.concurrent.atomic.AtomicLong(1000L);

    private Application newApplication() {
        Application a = new Application();
        a.setCustomerId(1L);
        a.setInsurerId(10L);
        a.setPlanId(100L);
        a.setQuoteId(quoteSeq.getAndIncrement());
        a.setProductType("TWO_WHEELER");
        a.setVehicleRegNo("KA22AB1234");
        a.setPremium(new BigDecimal("12000.00"));
        return applications.saveAndFlush(a);
    }

    private Policy newPolicy(String policyNo, Long applicationId, Long previousPolicyId) {
        Policy p = new Policy();
        p.setPolicyNo(policyNo);
        p.setApplicationId(applicationId);
        p.setPreviousPolicyId(previousPolicyId);
        p.setCustomerId(1L);
        p.setInsurerId(10L);
        p.setPlanId(100L);
        p.setVehicleRegNo("KA22AB1234");
        p.setPremiumPaid(new BigDecimal("12000.00"));
        p.setStartDate(LocalDate.of(2026, 10, 20));
        p.setEndDate(LocalDate.of(2027, 10, 19));
        return p;
    }

    @Test
    void applicationStartsSubmittedAndKeepsItsHistory() {
        Application app = newApplication();
        assertEquals(ApplicationStatus.SUBMITTED, app.getStatus());

        history.save(new ApplicationHistory(app.getId(), null, ApplicationStatus.SUBMITTED, null, "created"));
        history.saveAndFlush(new ApplicationHistory(app.getId(), ApplicationStatus.SUBMITTED,
                ApplicationStatus.APPROVED, null, "auto-approved"));

        List<ApplicationHistory> rows = history.findByApplicationIdOrderByIdAsc(app.getId());
        assertEquals(2, rows.size());
        assertNull(rows.get(0).getFromStatus());
        assertEquals(ApplicationStatus.APPROVED, rows.get(1).getToStatus());
    }

    @Test
    void policyNumberMustBeUnique() {
        policies.saveAndFlush(newPolicy("POL-2026-000001", newApplication().getId(), null));
        Policy duplicate = newPolicy("POL-2026-000001", newApplication().getId(), null);
        assertThrows(DataIntegrityViolationException.class, () -> policies.saveAndFlush(duplicate));
    }

    @Test
    void quoteIdMustBeUnique() {
        Application first = newApplication();
        Application duplicate = new Application();
        duplicate.setCustomerId(1L);
        duplicate.setInsurerId(10L);
        duplicate.setPlanId(100L);
        duplicate.setQuoteId(first.getQuoteId());
        duplicate.setProductType("TWO_WHEELER");
        duplicate.setVehicleRegNo("KA22AB1234");
        duplicate.setPremium(new BigDecimal("12000.00"));
        assertThrows(DataIntegrityViolationException.class, () -> applications.saveAndFlush(duplicate));
    }

    @Test
    void onlyOnePolicyPerApplication() {
        Long applicationId = newApplication().getId();
        policies.saveAndFlush(newPolicy("POL-2026-000002", applicationId, null));
        Policy second = newPolicy("POL-2026-000003", applicationId, null);
        assertThrows(DataIntegrityViolationException.class, () -> policies.saveAndFlush(second));
    }

    @Test
    void renewalsLinkToThePreviousPolicyAndNeedNoApplication() {
        Policy original = policies.saveAndFlush(newPolicy("POL-2026-000004", newApplication().getId(), null));
        Policy renewal1 = policies.saveAndFlush(newPolicy("POL-2026-000005", null, original.getId()));
        Policy renewal2 = policies.saveAndFlush(newPolicy("POL-2026-000006", null, renewal1.getId()));

        assertEquals(original.getId(), renewal1.getPreviousPolicyId());
        assertEquals(renewal1.getId(), renewal2.getPreviousPolicyId());
        assertNull(renewal2.getApplicationId());
    }

    @Test
    void cancellationRequestStartsRequestedAndLinksToAPolicy() {
        Policy policy = policies.saveAndFlush(newPolicy("POL-2026-000007", newApplication().getId(), null));

        CancellationRequest request = new CancellationRequest();
        request.setPolicyId(policy.getId());
        request.setInsurerId(policy.getInsurerId());
        request.setRequestedBy(1L);
        request.setReason("Sold the vehicle");
        CancellationRequest saved = cancellations.saveAndFlush(request);

        assertEquals(CancellationStatus.REQUESTED, saved.getStatus());
        assertEquals(policy.getId(), saved.getPolicyId());
        assertNull(saved.getRefundAmount());
    }
}
