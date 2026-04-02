package com.pmei.stock.controller;

import com.pmei.security.CustomUserPrincipal;
import com.pmei.stock.dto.ProductResponseDTO;
import com.pmei.stock.dto.StockAdjustmentRequest;
import com.pmei.stock.dto.StockMovementResponseDTO;
import com.pmei.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller responsible for stock management.
 *
 * Provides endpoints for:
 * - Adding stock (entry)
 * - Removing stock (exit)
 * - Monitoring low stock products
 * - Viewing stock movements history
 */
@Tag(name = "Stock", description = "Stock management endpoints")
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    /**
     * Adds stock to a product.
     *
     * @param productId Product ID
     * @param request Quantity to add
     * @param principal Authenticated user
     */
    @Operation(summary = "Add stock to a product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock added successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping("/entry/{productId}")
    public ResponseEntity<String> addStock(
            @PathVariable UUID productId,
            @RequestBody @Valid StockAdjustmentRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        UUID companyId = principal.getCompanyId();

        stockService.addStock(productId, request.quantity(), companyId);

        return ResponseEntity.ok("Stock added successfully.");
    }

    /**
     * Removes stock from a product.
     *
     * @param productId Product ID
     * @param request Quantity to remove
     * @param principal Authenticated user
     */
    @Operation(summary = "Remove stock from a product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock removed successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient stock"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping("/exit/{productId}")
    public ResponseEntity<String> removeStock(
            @PathVariable UUID productId,
            @RequestBody @Valid StockAdjustmentRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        UUID companyId = principal.getCompanyId();

        stockService.removeStock(productId, request.quantity(), companyId);

        return ResponseEntity.ok("Stock removed successfully.");
    }

    /**
     * Returns products with low stock.
     *
     * @param principal Authenticated user
     * @return List of low stock products
     */
    @Operation(summary = "Get low stock products")
    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductResponseDTO>> getLowStockProducts(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        UUID companyId = principal.getCompanyId();

        return ResponseEntity.ok(
                stockService.getLowStockProducts(companyId)
        );
    }

    /**
     * Returns stock movements for a product.
     *
     * @param productId Product ID
     * @param principal Authenticated user
     * @return List of stock movements
     */
    @Operation(summary = "Get stock movements by product")
    @GetMapping("/movements/{productId}")
    public ResponseEntity<List<StockMovementResponseDTO>> getMovements(
            @PathVariable UUID productId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        UUID companyId = principal.getCompanyId();

        return ResponseEntity.ok(
                stockService.getProductMovements(productId, companyId)
        );
    }
}
