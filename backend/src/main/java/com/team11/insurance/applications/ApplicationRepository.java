package com.team11.insurance.applications;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByInsurerIdAndStatus(Long insurerId, ApplicationStatus status);

    List<Application> findByStatus(ApplicationStatus status);
}
