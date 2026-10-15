package com.team11.insurance.applications;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationHistoryRepository extends JpaRepository<ApplicationHistory, Long> {

    List<ApplicationHistory> findByApplicationIdOrderByIdAsc(Long applicationId);
}
