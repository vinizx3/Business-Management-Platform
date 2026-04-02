package com.pmei.stock.repository;

import com.pmei.stock.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository responsible for Supplier data access.
 *
 * Currently not used, but will support:
 * - Supplier management
 * - Product sourcing
 * - Purchase flows
 */
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
}
