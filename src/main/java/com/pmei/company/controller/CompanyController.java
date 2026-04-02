package com.pmei.company.controller;

import com.pmei.company.dto.CompanyResponseDTO;
import com.pmei.company.model.Company;
import com.pmei.company.repository.CompanyRepository;
import com.pmei.company.service.CompanyService;
import com.pmei.security.CustomUserPrincipal;
import com.pmei.shared.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for company-related operations.
 *
 * Provides endpoints to retrieve company information
 * based on the authenticated user.
 */
@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    /**
     * Retrieves the company associated with the authenticated user.
     *
     * @param principal Authenticated user principal
     * @return Company details
     */
    @Operation(summary = "Get authenticated user's company")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Company retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Company not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<CompanyResponseDTO> getMyCompany(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                companyService.getById(principal.getCompanyId())
        );
    }
}
