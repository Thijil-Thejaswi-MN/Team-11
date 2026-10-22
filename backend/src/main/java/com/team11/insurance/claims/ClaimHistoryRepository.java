package com.team11.insurance.claims;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimHistoryRepository extends JpaRepository<ClaimHistory, Long> {

    List<ClaimHistory> findByClaimIdOrderByIdAsc(Long claimId);
}
