package com.team11.insurance.policies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.common.exception.AccessDeniedException;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.filestore.DocumentCategory;
import com.team11.insurance.filestore.IFileStore;
import com.team11.insurance.filestore.StoredDocument;
import com.team11.insurance.notifications.INotify;
import com.team11.insurance.notifications.NotificationEvent;
import com.team11.insurance.plans.PlanRepository;
import com.team11.insurance.policies.dto.PolicySummaryResponse;

class PolicyServiceTest {

    private PolicyRepository policyRepository;
    private PolicyPdfGenerator policyPdfGenerator;
    private IFileStore fileStore;
    private INotify notify;
    private Clock fixedClock;
    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyRepository = mock(PolicyRepository.class);
        policyPdfGenerator = new PolicyPdfGenerator();
        fileStore = mock(IFileStore.class);
        notify = mock(INotify.class);
        fixedClock = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneId.of("UTC"));

        ObjectProvider<INotify> notifyProvider = createSimpleProvider(notify);
        ObjectProvider<PlanRepository> planProvider = createSimpleProvider(null);
        ObjectProvider<com.team11.insurance.auth.repository.UserRepository> userProvider = createSimpleProvider(null);
        ObjectProvider<Clock> clockProvider = createSimpleProvider(fixedClock);

        policyService = new PolicyService(
                policyRepository,
                policyPdfGenerator,
                fileStore,
                notifyProvider,
                planProvider,
                userProvider,
                clockProvider
        );
    }

    private <T> ObjectProvider<T> createSimpleProvider(T instance) {
        return new ObjectProvider<>() {
            @Override public T getObject(Object... args) { return instance; }
            @Override public T getIfAvailable() { return instance; }
            @Override public T getIfUnique() { return instance; }
            @Override public T getObject() { return instance; }
        };
    }

    @Test
    @DisplayName("Issue policy creates active policy with OpenPDF schedule and notification")
    void testIssuePolicy_Success() {
        Application app = new Application();
        app.setId(105L);
        app.setCustomerId(50L);
        app.setInsurerId(2L);
        app.setPlanId(10L);
        app.setVehicleRegNo("KA-05-MM-9999");
        app.setStatus(ApplicationStatus.PAID);

        when(policyRepository.findByApplicationId(105L)).thenReturn(Optional.empty());

        StoredDocument storedDoc = new StoredDocument(
                999L, "uuid.pdf", "policy-POL-2026-000105.pdf", "application/pdf",
                3000L, DocumentCategory.POLICY, "sha256", 50L, Instant.now()
        );
        when(fileStore.storeStream(any(InputStream.class), anyString(), eq("application/pdf"), anyLong(), eq(DocumentCategory.POLICY), eq(50L)))
                .thenReturn(storedDoc);

        when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));

        Policy issued = policyService.issuePolicy(app, new BigDecimal("12500.00"));

        assertThat(issued).isNotNull();
        assertThat(issued.getPolicyNo()).isEqualTo("POL-2026-000105");
        assertThat(issued.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(issued.getDocumentFileId()).isEqualTo(999L);
        assertThat(issued.getStartDate()).isEqualTo(LocalDate.of(2026, 10, 23));
        assertThat(issued.getEndDate()).isEqualTo(LocalDate.of(2027, 10, 22));

        verify(notify).send(eq(NotificationEvent.POLICY_ISSUED), eq(50L), any());
    }

    @Test
    @DisplayName("Issue policy is idempotent: returns existing policy if already issued")
    void testIssuePolicy_Idempotent() {
        Application app = new Application();
        app.setId(105L);

        Policy existing = new Policy();
        existing.setPolicyNo("POL-2026-000105");
        existing.setStatus(PolicyStatus.ACTIVE);
        when(policyRepository.findByApplicationId(105L)).thenReturn(Optional.of(existing));

        Policy result = policyService.issuePolicy(app, new BigDecimal("12500.00"));

        assertThat(result).isSameAs(existing);
    }

    @Test
    @DisplayName("Get policies filters by customer ID for CUSTOMER role")
    void testGetPolicies_CustomerFilter() {
        CurrentUser customer = new CurrentUser(50L, UserRole.CUSTOMER, null);
        Policy p1 = new Policy();
        p1.setPolicyNo("POL-2026-000001");
        p1.setCustomerId(50L);

        when(policyRepository.findByCustomerIdOrderByCreatedAtDesc(50L)).thenReturn(List.of(p1));

        List<PolicySummaryResponse> results = policyService.getPolicies(customer, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).policyNo()).isEqualTo("POL-2026-000001");
    }

    @Test
    @DisplayName("Get policy by ID enforces customer ownership")
    void testGetPolicyById_AccessDeniedForOtherCustomer() {
        CurrentUser customerA = new CurrentUser(50L, UserRole.CUSTOMER, null);
        Policy policyOwnedByB = new Policy();
        policyOwnedByB.setCustomerId(99L);

        when(policyRepository.findById(1L)).thenReturn(Optional.of(policyOwnedByB));

        assertThatThrownBy(() -> policyService.getPolicyById(1L, customerA))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Get non-existent policy throws ResourceNotFoundException")
    void testGetPolicyById_NotFound() {
        CurrentUser customer = new CurrentUser(50L, UserRole.CUSTOMER, null);
        when(policyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.getPolicyById(999L, customer))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
