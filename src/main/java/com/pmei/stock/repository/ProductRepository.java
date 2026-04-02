package com.pmei.stock.repository;

import com.pmei.stock.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository responsible for Product data access.
 */
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Repository responsible for Product data access.
     */
    List<Product> findAllByCompanyId(UUID companyId);

    /**
     * Returns all products for a company.
     */
    Optional<Product> findByIdAndCompanyId(UUID id, UUID companyId);

    /**
     * Finds a product by ID and company.
     */
    @Query("SELECT p FROM Product p WHERE p.quantity < p.minimumStock AND p.company.id = :companyId")
    List<Product> findLowStockProducts(UUID companyId);
}
