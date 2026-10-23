package com.team11.insurance.policies;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByApplicationId(Long applicationId);

    Optional<Policy> findByPolicyNo(String policyNo);

    boolean existsByPolicyNo(String policyNo);

    boolean existsByApplicationId(Long applicationId);

    List<Policy> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Policy> findByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId, PolicyStatus status);

    List<Policy> findByInsurerIdOrderByCreatedAtDesc(Long insurerId);

    List<Policy> findByInsurerIdAndStatusOrderByCreatedAtDesc(Long insurerId, PolicyStatus status);

    List<Policy> findByStatusOrderByCreatedAtDesc(PolicyStatus status);

    List<Policy> findAllByOrderByCreatedAtDesc();

    boolean existsByVehicleRegNoIgnoreCaseAndStatus(String vehicleRegNo, PolicyStatus status);
}
