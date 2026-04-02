package com.pmei.stock.service;

import com.pmei.shared.exception.BusinessException;
import com.pmei.stock.dto.ProductResponseDTO;
import com.pmei.stock.dto.StockMovementResponseDTO;
import com.pmei.stock.model.MovementType;
import com.pmei.stock.model.Product;
import com.pmei.stock.model.StockMovement;
import com.pmei.stock.repository.ProductRepository;
import com.pmei.stock.repository.StockMovementRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for managing stock operations.
 *
 * Handles:
 * - Stock addition and removal
 * - Stock movement tracking (history)
 * - Low stock alerts
 *
 * Ensures consistency between product quantity and stock movements.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StockService {

    private final ProductService productService;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final Clock clock;

    /**
     * Adds stock to a product.
     */
    public void addStock(UUID productId, Integer quantity, UUID companyId){

        Product product = productService.findProductOrThrow(productId, companyId);

        product.setQuantity(product.getQuantity() + quantity);

        // Register stock entry movement
        saveMovement(product, quantity, MovementType.ENTRY);
        checkMinimumStock(product);

        log.info("Stock added | product={} | quantity={}", product.getId(), quantity);
    }

    /**
     * Removes stock from a product.
     *
     * Rule:
     * - Cannot remove more than available stock
     */
    public void removeStock(UUID productId, Integer quantity, UUID companyId){

        Product product = productService.findProductOrThrow(productId, companyId);

        if (product.getQuantity() < quantity) {
            throw new BusinessException("Insufficient stock for product: " + product.getName());
        }

        product.setQuantity(product.getQuantity() - quantity);

        // Register stock exit movement
        saveMovement(product, quantity, MovementType.EXIT);
        checkMinimumStock(product);

        log.info("Stock removed | product={} | quantity={}", product.getId(), quantity);
    }

    /**
     * Removes stock and returns updated product.
     *
     * Used in sales flow.
     */
    public Product removeStockAndReturn(UUID productId, Integer quantity, UUID companyId) {
        removeStock(productId, quantity, companyId);
        return productService.findProductOrThrow(productId, companyId);
    }

    /**
     * Returns all stock movements for a product.
     */
    public List<StockMovementResponseDTO> getProductMovements(UUID productId, UUID companyId){

        // Ensure product belongs to company
        productService.findProductOrThrow(productId, companyId);

        return stockMovementRepository.findByProductId(productId).stream()
                .map(m -> new StockMovementResponseDTO(
                                m.getId(),
                                m.getProduct().getId(),
                                m.getProduct().getName(),
                                m.getQuantity(),
                                m.getType(),
                                m.getDate()
                        )
                ).toList();
    }

    /**
     * Returns products below minimum stock level.
     */
    public List<ProductResponseDTO> getLowStockProducts(UUID companyId) {
        return productRepository.findLowStockProducts(companyId)
                .stream()
                .map(product -> new ProductResponseDTO(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        product.getQuantity(),
                        product.getMinimumStock()
                ))
                .toList();
    }

    /**
     * Saves a stock movement (entry or exit).
     */
    private void saveMovement(Product product, Integer quantity, MovementType type){
        StockMovement movement = StockMovement.builder()
                .product(product)
                .quantity(quantity)
                .type(type)
                .date(LocalDateTime.now(clock))
                .build();
        stockMovementRepository.save(movement);
    }

    /**
     * Logs a warning if product stock is below minimum level.
     */
    private void checkMinimumStock(Product product){
        if (product.getQuantity() < product.getMinimumStock()) {
            log.warn("Low stock alert | product={} | current={} | minimum={}",
                    product.getName(),
                    product.getQuantity(),
                    product.getMinimumStock());
        }
    }
}