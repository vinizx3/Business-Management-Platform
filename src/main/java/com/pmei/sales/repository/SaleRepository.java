package com.pmei.sales.repository;

import com.pmei.sales.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository responsible for Sale data access.
 */
public interface SaleRepository extends JpaRepository<Sale, UUID> {

    /**
     * Returns all sales for a given company.
     */
    List<Sale> findAllByCompanyId(UUID companyId);

    /**
            * Finds a sale by ID and company.
     */
    Optional<Sale> findByIdAndCompanyId(UUID id, UUID companyId);
}
