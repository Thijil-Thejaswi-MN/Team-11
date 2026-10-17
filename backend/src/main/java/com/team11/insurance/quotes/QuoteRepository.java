package com.team11.insurance.quotes;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for Quote entities.
 */
@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {

    List<Quote> findByCustomerId(Long customerId);

    List<Quote> findByCustomerIdAndProductType(Long customerId, String productType);

    List<Quote> findByPlanId(Long planId);
}
