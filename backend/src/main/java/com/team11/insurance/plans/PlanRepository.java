package com.team11.insurance.plans;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Plan} entities.
 */
@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByCode(String code);

    List<Plan> findByInsurerId(Long insurerId);

    List<Plan> findByType(String type);

    List<Plan> findByActiveTrue();

    List<Plan> findByInsurerIdAndActiveTrue(Long insurerId);

    List<Plan> findByTypeAndActiveTrue(String type);

    boolean existsByCodeIgnoreCase(String code);
}
