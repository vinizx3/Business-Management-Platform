package com.pmei.sales.repository;

import com.pmei.sales.model.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/**
 * Repository for SaleItem entity.
 *
 * Currently not used directly, but can be extended for:
 * - Sales analytics
 * - Product performance reports
 * - Historical data queries
 */
public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {
}
