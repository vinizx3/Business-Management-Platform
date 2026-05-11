package com.pmei.company.repository;

import com.pmei.company.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository responsible for Company data access.
 */
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    /**
     * Repository responsible for Company data access.
     */
    Optional<Company> findByDocument(String document);
}
