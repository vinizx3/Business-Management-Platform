package com.pmei.contracts.repository;

import com.pmei.contracts.model.ContractAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository responsible for Contract Adjustment data access.
 */
public interface ContractAdjustmentRepository extends JpaRepository<ContractAdjustment, UUID> {
    /**
     * Returns all adjustments associated with a contract.
     */
    List<ContractAdjustment> findAllByContractId(UUID contractId);
}
