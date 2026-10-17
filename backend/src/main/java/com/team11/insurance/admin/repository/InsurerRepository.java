package com.team11.insurance.admin.repository;

import com.team11.insurance.admin.entity.Insurer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Insurer} entities.
 */
@Repository
public interface InsurerRepository extends JpaRepository<Insurer, Long> {

    Optional<Insurer> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
