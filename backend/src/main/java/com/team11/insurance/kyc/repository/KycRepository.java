package com.team11.insurance.kyc.repository;

import com.team11.insurance.kyc.KycStatus;
import com.team11.insurance.kyc.entity.KycRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link KycRecord} entities.
 */
@Repository
public interface KycRepository extends JpaRepository<KycRecord, Long> {

    Optional<KycRecord> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<KycRecord> findByStatus(KycStatus status);
}
