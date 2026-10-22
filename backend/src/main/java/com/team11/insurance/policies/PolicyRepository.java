package com.team11.insurance.policies;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByApplicationId(Long applicationId);
}

