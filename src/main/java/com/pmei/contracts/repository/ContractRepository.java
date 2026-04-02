package com.pmei.contracts.repository;

import com.pmei.contracts.model.Contract;
import com.pmei.contracts.model.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository responsible for accessing Contract data.
 *
 * Provides methods for:
 * - Basic CRUD operations (via JpaRepository)
 * - Custom queries for business-specific use cases
 */
public interface ContractRepository extends JpaRepository<Contract, UUID> {

    /**
     * Returns all contracts for a given company.
     */
    List<Contract> findAllByCompanyId(UUID companyId);

    /**
     * Finds a contract by ID and ensures it belongs to the company.
     */
    Optional<Contract> findByIdAndCompanyId(UUID id, UUID companyId);

    /**
     * Returns contracts that will expire within a given period.
     *
     * Only ACTIVE contracts are considered.
     *
     * @param companyId Company identifier
     * @param today Current date
     * @param limitDate Upper limit date
     * @return List of expiring contracts
     */
    @Query("""
        SELECT c FROM Contract c
        WHERE c.company.id = :companyId
        AND c.status = 'ACTIVE'
        AND c.endDate BETWEEN :today AND :limitDate
    """)
    List<Contract> findExpiringContracts(
            UUID companyId,
            LocalDate today,
            LocalDate limitDate
    );

    /**
     * Calculates the total monthly value of active contracts by type.
     *
     * Used to determine financial impact:
     * - CLIENT → income
     * - SUPPLIER → expense
     *
     * @return Sum of monthly values (defaults to 0 if none)
     */
    @Query("""
        SELECT COALESCE(SUM(c.monthlyValue), 0)
        FROM Contract c
        WHERE c.company.id = :companyId
        AND c.type = :type
        AND c.status = 'ACTIVE'
    """)
    BigDecimal sumActiveByType(UUID companyId, ContractType type);

}
