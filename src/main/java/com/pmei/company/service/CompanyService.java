package com.pmei.company.service;

import com.pmei.company.dto.CompanyResponseDTO;
import com.pmei.company.model.Company;
import com.pmei.company.repository.CompanyRepository;
import com.pmei.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service responsible for company-related operations.
 *
 * Handles:
 * - Company retrieval
 * - Business validations related to company data
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    /**
     * Retrieves a company by its ID.
     *
     * @param companyId Company identifier
     * @return CompanyResponseDTO with company details
     * @throws ResourceNotFoundException if company is not found
     */
    public CompanyResponseDTO getById(UUID companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Company not found: " + companyId)
                );

        return toDTO(company);
    }

    /**
     * Converts Company entity to DTO.
     */
    private CompanyResponseDTO toDTO(Company company) {
        return new CompanyResponseDTO(
                company.getId(),
                company.getName(),
                company.getDocument(),
                company.getEmail(),
                company.getPhone()
        );
    }
}
