package com.pmei.sales.controller;

import com.pmei.sales.dto.SaleRequestDTO;
import com.pmei.sales.dto.SaleResponseDTO;
import com.pmei.sales.service.SaleService;
import com.pmei.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller responsible for sales management.
 *
 * Provides endpoints for:
 * - Creating sales
 * - Retrieving sales by ID
 * - Listing all sales of a company
 */
@Tag(name = "Sales", description = "Sales management endpoints")
@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    /**
     * Creates a new sale.
     *
     * @param dto Sale request data
     * @param principal Authenticated user
     * @return Created sale
     */
    @Operation(summary = "Create a new sale")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sale created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Company or product not found")
    })
    @PostMapping
    public ResponseEntity<SaleResponseDTO> create(
            @RequestBody @Valid SaleRequestDTO dto,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saleService.createSale(dto, principal.getCompanyId()));
    }

    /**
     * Retrieves a sale by ID.
     *
     * @param id Sale ID
     * @param principal Authenticated user
     * @return Sale data
     */
    @Operation(summary = "Get sale by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sale found"),
            @ApiResponse(responseCode = "404", description = "Sale not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SaleResponseDTO> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(saleService.findById(id, principal.getCompanyId()));
    }

    /**
     * Retrieves all sales for the authenticated company.
     *
     * @param principal Authenticated user
     * @return List of sales
     */
    @Operation(summary = "Get all sales")
    @GetMapping
    public ResponseEntity<List<SaleResponseDTO>> findAll(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(saleService.findAll(principal.getCompanyId()));
    }
}
