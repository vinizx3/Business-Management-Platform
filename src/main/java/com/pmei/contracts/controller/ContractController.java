package com.pmei.contracts.controller;

import com.pmei.contracts.dto.*;
import com.pmei.contracts.service.ContractService;
import com.pmei.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller responsible for contract management.
 *
 * Provides endpoints for:
 * - Creating and retrieving contracts
 * - Monitoring expiration
 * - Cancelling contracts
 * - Managing contract adjustments
 * - Analyzing financial impact
 */
@Tag(name = "Contracts", description = "Contract management endpoints")
@Slf4j
@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    /**
     * Creates a new contract.
     */
    @Operation(summary = "Create a new contract")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Contract created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    @PostMapping
    public ResponseEntity<ContractResponseDTO> create(
            @RequestBody @Valid ContractRequestDTO dto,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contractService.create(dto, principal.getCompanyId()));
    }

    /**
     * Returns all contracts for the authenticated company.
     */
    @Operation(summary = "Get all contracts")
    @GetMapping
    public ResponseEntity<List<ContractResponseDTO>> findAll(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(contractService.findAll(principal.getCompanyId()));
    }

    /**
     * Returns a contract by ID.
     */
    @Operation(summary = "Get contract by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contract found"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ContractResponseDTO> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(contractService.findById(id, principal.getCompanyId()));
    }

    /**
     * Returns contracts that will expire within a given number of days.
     *
     * Default: 30 days
     */
    @Operation(summary = "Get expiring contracts")
    @GetMapping("/expiring")
    public ResponseEntity<List<ContractResponseDTO>> findExpiring(
            @RequestParam(defaultValue = "30") int daysAhead,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                contractService.findExpiring(principal.getCompanyId(), daysAhead));
    }

    /**
     * Cancels a contract.
     */
    @Operation(summary = "Cancel a contract")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contract cancelled"),
            @ApiResponse(responseCode = "400", description = "Contract already cancelled"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ContractResponseDTO> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(contractService.cancel(id, principal.getCompanyId()));
    }

    /**
     * Applies an adjustment to a contract.
     */
    @Operation(summary = "Adjust contract value")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Adjustment applied"),
            @ApiResponse(responseCode = "400", description = "Invalid adjustment"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    @PostMapping("/{id}/adjustments")
    public ResponseEntity<ContractAdjustmentResponseDTO> adjust(
            @PathVariable UUID id,
            @RequestBody @Valid ContractAdjustmentRequestDTO dto,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contractService.adjust(id, dto, principal.getCompanyId()));
    }

    /**
     * Returns all adjustments for a contract.
     */
    @Operation(summary = "Get contract adjustments")
    @GetMapping("/{id}/adjustments")
    public ResponseEntity<List<ContractAdjustmentResponseDTO>> findAdjustments(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                contractService.findAdjustments(id, principal.getCompanyId()));
    }

    /**
     * Returns the financial impact of active contracts.
     */
    @Operation(summary = "Get contracts cash flow impact")
    @GetMapping("/cash-flow")
    public ResponseEntity<ContractCashFlowDTO> getCashFlowImpact(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                contractService.getCashFlowImpact(principal.getCompanyId()));
    }
}
