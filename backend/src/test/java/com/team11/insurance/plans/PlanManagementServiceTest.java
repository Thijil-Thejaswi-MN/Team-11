package com.team11.insurance.plans;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.admin.entity.Insurer;
import com.team11.insurance.admin.repository.InsurerRepository;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.common.exception.AccessDeniedException;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.common.exception.StateConflictException;
import com.team11.insurance.common.exception.ValidationException;
import com.team11.insurance.plans.dto.CreatePlanRequest;
import com.team11.insurance.plans.dto.PatchPlanRequest;
import com.team11.insurance.plans.dto.PlanCompareResponse;
import com.team11.insurance.plans.dto.PlanCreateResponse;
import com.team11.insurance.plans.dto.PlanListResponse;
import com.team11.insurance.plans.dto.PlanResponse;
import com.team11.insurance.plans.dto.UpdatePlanRequest;

@ExtendWith(MockitoExtension.class)
class PlanManagementServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private InsurerRepository insurerRepository;

    private ObjectMapper objectMapper;
    private PlanManagementService service;

    private final CurrentUser insurerAAdmin = new CurrentUser(2001L, UserRole.INSURER_ADMIN, 1L);
    private final CurrentUser insurerBAdmin = new CurrentUser(2002L, UserRole.INSURER_ADMIN, 2L);
    private final CurrentUser customer = new CurrentUser(1001L, UserRole.CUSTOMER, null);

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new PlanManagementService(planRepository, insurerRepository, objectMapper);
    }

    @Test
    @DisplayName("Create plan: authorized Insurer Admin creates plan successfully")
    void testCreatePlan_Success() {
        CreatePlanRequest req = new CreatePlanRequest(
                "PLAN-A-M3", "4W", "SafeShield Gold 4W",
                new BigDecimal("600000"), new BigDecimal("1000"), 0,
                new BigDecimal("3000"), "{}", "{}", List.of("ZERO_DEP"), true
        );

        when(planRepository.existsByCodeIgnoreCase("PLAN-A-M3")).thenReturn(false);
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> {
            Plan p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        PlanCreateResponse resp = service.createPlan(req, insurerAAdmin);

        assertNotNull(resp);
        assertEquals(10L, resp.getPlanId());
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    @DisplayName("Create plan: duplicate code throws StateConflictException")
    void testCreatePlan_DuplicateCode_ThrowsConflict() {
        CreatePlanRequest req = new CreatePlanRequest(
                "PLAN-A-M1", "4W", "SafeShield",
                new BigDecimal("500000"), BigDecimal.ZERO, 0,
                new BigDecimal("2000"), "{}", "{}", List.of(), true
        );

        when(planRepository.existsByCodeIgnoreCase("PLAN-A-M1")).thenReturn(true);

        assertThrows(StateConflictException.class, () -> service.createPlan(req, insurerAAdmin));
    }

    @Test
    @DisplayName("Create plan: Customer role is denied with AccessDeniedException")
    void testCreatePlan_CustomerDenied() {
        CreatePlanRequest req = new CreatePlanRequest(
                "PLAN-A-M3", "4W", "SafeShield",
                new BigDecimal("500000"), BigDecimal.ZERO, 0,
                new BigDecimal("2000"), "{}", "{}", List.of(), true
        );

        assertThrows(AccessDeniedException.class, () -> service.createPlan(req, customer));
    }

    @Test
    @DisplayName("Update plan: Insurer Admin can update own insurer's plan")
    void testUpdatePlan_Success() {
        Plan existing = new Plan("PLAN-A-M1", 1L, "MOTOR_FOUR_WHEELER", "SafeShield Car",
                new BigDecimal("500000"), BigDecimal.ZERO, 0,
                new BigDecimal("2000"), "{}", "{}", "[]", true);
        existing.setId(1L);

        when(planRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(planRepository.save(any(Plan.class))).thenReturn(existing);
        when(insurerRepository.findById(1L)).thenReturn(Optional.of(new Insurer("SafeShield")));

        UpdatePlanRequest req = new UpdatePlanRequest();
        req.setName("SafeShield Car Updated");
        req.setType("MOTOR_FOUR_WHEELER");
        req.setCoverage(new BigDecimal("550000"));
        req.setDeductible(new BigDecimal("500"));
        req.setBasePremium(new BigDecimal("2200"));
        req.setActive(true);

        PlanResponse resp = service.updatePlan(1L, req, insurerAAdmin);

        assertEquals("SafeShield Car Updated", resp.getName());
        assertEquals(new BigDecimal("550000"), resp.getCoverage());
    }

    @Test
    @DisplayName("Update plan: Cross-insurer modification is denied (B-ADM on A's plan)")
    void testUpdatePlan_CrossInsurerDenied() {
        Plan planA = new Plan("PLAN-A-M1", 1L, "MOTOR_FOUR_WHEELER", "SafeShield Car",
                new BigDecimal("500000"), BigDecimal.ZERO, 0,
                new BigDecimal("2000"), "{}", "{}", "[]", true);
        planA.setId(1L);

        when(planRepository.findById(1L)).thenReturn(Optional.of(planA));

        UpdatePlanRequest req = new UpdatePlanRequest();
        req.setName("Tampered Name");
        req.setType("4W");
        req.setCoverage(new BigDecimal("500000"));
        req.setDeductible(BigDecimal.ZERO);
        req.setBasePremium(new BigDecimal("2000"));

        assertThrows(AccessDeniedException.class, () -> service.updatePlan(1L, req, insurerBAdmin));
    }

    @Test
    @DisplayName("Patch plan: deactivating plan sets active=false")
    void testPatchPlan_Deactivate() {
        Plan existing = new Plan("PLAN-A-M1", 1L, "MOTOR_FOUR_WHEELER", "SafeShield Car",
                new BigDecimal("500000"), BigDecimal.ZERO, 0,
                new BigDecimal("2000"), "{}", "{}", "[]", true);
        existing.setId(1L);

        when(planRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        PatchPlanRequest patch = new PatchPlanRequest(false);
        PlanResponse resp = service.patchPlan(1L, patch, insurerAAdmin);

        assertFalse(resp.isActive());
    }

    @Test
    @DisplayName("Public browse plans: returns only active plans matching filters")
    void testGetPublicPlans_Filter() {
        Plan activeHealth = new Plan("PLAN-A-H1", 1L, "HEALTH", "Health 1",
                new BigDecimal("500000"), BigDecimal.ZERO, 30, new BigDecimal("8500"), "{}", "{}", "[]", true);
        activeHealth.setId(1L);

        Plan activeMotor = new Plan("PLAN-A-M1", 1L, "MOTOR_FOUR_WHEELER", "Car 1",
                new BigDecimal("400000"), BigDecimal.ZERO, 0, new BigDecimal("2000"), "{}", "{}", "[]", true);
        activeMotor.setId(2L);

        when(planRepository.findByActiveTrue()).thenReturn(List.of(activeHealth, activeMotor));

        PlanListResponse resp = service.getPublicPlans("HEALTH", null, new BigDecimal("10000"), new BigDecimal("300000"), 1, 10);

        assertEquals(1, resp.getTotal());
        assertEquals("PLAN-A-H1", resp.getItems().get(0).getCode());
    }

    @Test
    @DisplayName("Compare plans: up to 3 plans compared successfully")
    void testComparePlans_Success() {
        Plan p1 = new Plan("PLAN-A-M1", 1L, "MOTOR_FOUR_WHEELER", "Car 1", new BigDecimal("400000"), BigDecimal.ZERO, 0, new BigDecimal("2000"), "{}", "{}", "[]", true);
        p1.setId(1L);
        Plan p2 = new Plan("PLAN-A-M2", 1L, "MOTOR_TWO_WHEELER", "Bike 1", new BigDecimal("100000"), BigDecimal.ZERO, 0, new BigDecimal("500"), "{}", "{}", "[]", true);
        p2.setId(2L);

        when(planRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(planRepository.findById(2L)).thenReturn(Optional.of(p2));

        PlanCompareResponse resp = service.comparePlans(List.of(1L, 2L));

        assertEquals(2, resp.getPlans().size());
    }

    @Test
    @DisplayName("Compare plans: more than 3 plans throws IllegalArgumentException")
    void testComparePlans_MoreThanThree_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.comparePlans(List.of(1L, 2L, 3L, 4L)));
    }

    @Test
    @DisplayName("Compare plans: non-existent plan throws ResourceNotFoundException")
    void testComparePlans_NotFound_ThrowsException() {
        when(planRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.comparePlans(List.of(999L)));
    }
}
