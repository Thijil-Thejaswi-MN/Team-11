package com.team11.insurance.claims;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByClaimNo(String claimNo);

    List<Claim> findByPolicyId(Long policyId);

    List<Claim> findByCustomerId(Long customerId);

    List<Claim> findByInsurerId(Long insurerId);

    List<Claim> findByInsurerIdAndStatus(Long insurerId, ClaimStatus status);

    boolean existsByPolicyIdAndStatus(Long policyId, ClaimStatus status);

    boolean existsByPolicyIdAndIncidentDateAndClaimAmount(Long policyId, LocalDate incidentDate, BigDecimal claimAmount);
}
