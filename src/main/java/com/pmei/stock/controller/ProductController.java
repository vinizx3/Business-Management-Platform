package com.pmei.stock.controller;

import com.pmei.security.CustomUserPrincipal;
import com.pmei.stock.dto.ProductRequestDTO;
import com.pmei.stock.dto.ProductResponseDTO;
import com.pmei.stock.service.ProductService;
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
 * REST controller responsible for product management.
 *
 * Provides endpoints for:
 * - Creating products
 * - Updating products
 * - Deleting products
 * - Retrieving products
 */
@Tag(name = "Products", description = "Product management endpoints")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Creates a new product.
     */
    @Operation(summary = "Create a new product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    public ResponseEntity<ProductResponseDTO> create(
            @RequestBody @Valid ProductRequestDTO dto,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(dto, principal.getCompanyId()));
    }

    /**
     * Returns all products.
     */
    @Operation(summary = "Get all products")
    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> findAll(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(productService.findAll(principal.getCompanyId()));
    }

    /**
     * Returns a product by ID.
     */
    @Operation(summary = "Get product by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(productService.findById(id, principal.getCompanyId()));
    }

    /**
     * Updates a product.
     */
    @Operation(summary = "Update a product")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> update(
            @PathVariable UUID id,
            @RequestBody @Valid ProductRequestDTO dto,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(productService.update(id, dto, principal.getCompanyId()));
    }

    /**
     * Deletes a product.
     */
    @Operation(summary = "Delete a product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        productService.delete(id, principal.getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
