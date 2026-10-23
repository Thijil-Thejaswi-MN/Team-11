package com.team11.insurance.applications;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /** One application per quote: a second submission of the same quote is a conflict (409). */
    boolean existsByQuoteId(Long quoteId);

    List<Application> findByInsurerIdAndStatus(Long insurerId, ApplicationStatus status);

    List<Application> findByStatus(ApplicationStatus status);
}
