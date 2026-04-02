package com.pmei.stock.repository;

import com.pmei.stock.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository responsible for StockMovement data access.
 *
 * Used to track stock entry and exit history.
 */
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    /**
     * Repository responsible for StockMovement data access.
     *
     * Used to track stock entry and exit history.
     */
    List<StockMovement> findByProductId(UUID productId);
}
