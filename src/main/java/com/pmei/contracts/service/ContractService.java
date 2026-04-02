package com.pmei.contracts.service;

import com.pmei.company.model.Company;
import com.pmei.company.repository.CompanyRepository;
import com.pmei.contracts.dto.*;
import com.pmei.contracts.model.Contract;
import com.pmei.contracts.model.ContractAdjustment;
import com.pmei.contracts.model.ContractStatus;
import com.pmei.contracts.model.ContractType;
import com.pmei.contracts.repository.ContractAdjustmentRepository;
import com.pmei.contracts.repository.ContractRepository;
import com.pmei.financial.dto.FinancialRequest;
import com.pmei.financial.model.RecurrenceType;
import com.pmei.financial.model.TransactionType;
import com.pmei.financial.service.FinancialService;
import com.pmei.shared.exception.BusinessException;
import com.pmei.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for managing contracts and their financial impact.
 *
 * This service handles:
 * - Contract creation, cancellation and queries
 * - Contract adjustments (price changes)
 * - Expiration monitoring
 * - Financial integration (cash flow impact)
 *
 * Business rules are enforced here to ensure data consistency.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractAdjustmentRepository contractAdjustmentRepository;
    private final CompanyRepository companyRepository;
    private final FinancialService financialService;
    private final Clock clock;

    /**
     * Creates a new contract for a given company.
     *
     * Rules:
     * - Company must exist
     * - Dates must be valid
     * - Contract starts as ACTIVE
     * - Automatically registers financial impact
     */
    public ContractResponseDTO create(ContractRequestDTO dto, UUID companyId){

        // Ensure the company exists before creating the contract
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

        // Validate contract date consistency
        validateDates(dto.startDate(), dto.endDate());

        Contract contract = Contract.builder()
                .title(dto.title())
                .description(dto.description())
                .monthlyValue(dto.monthlyValue())
                .type(dto.type())
                .status(ContractStatus.ACTIVE)
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .alertDaysBefore(dto.alertDaysBefore())
                .company(company)
                .build();

        Contract saved = contractRepository.save(contract);

        // Register financial impact (income or expense)
        registerFinancialImpact(saved, companyId);

        log.info("Contract created | company={} | contract={} | type={} | value={}",
                companyId, saved.getId(), saved.getType(), saved.getMonthlyValue());

        return toDTO(saved);
    }

    /**
     * Returns all contracts for a company.
     */
    @Transactional(readOnly = true)
    public List<ContractResponseDTO> findAll(UUID companyId) {
        return contractRepository.findAllByCompanyId(companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Returns a contract by ID, ensuring it belongs to the company.
     */
    @Transactional(readOnly = true)
    public ContractResponseDTO findById(UUID id, UUID companyId) {
        Contract contract = findContractOrThrow(id, companyId);
        return toDTO(contract);
    }

    /**
     * Finds contracts that will expire within a given number of days.
     */
    @Transactional(readOnly = true)
    public List<ContractResponseDTO> findExpiring(UUID companyId, int daysAhead) {
        LocalDate today = LocalDate.now(clock);
        LocalDate limit = today.plusDays(daysAhead);

        return contractRepository.findExpiringContracts(companyId, today, limit)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Cancels a contract.
     *
     * Rule:
     * - Cannot cancel an already cancelled contract
     */
    public ContractResponseDTO cancel(UUID id, UUID companyId) {

        Contract contract = findContractOrThrow(id, companyId);

        if (contract.getStatus() == ContractStatus.CANCELLED) {
            throw new BusinessException("Contract is already cancelled.");
        }

        contract.setStatus(ContractStatus.CANCELLED);

        log.info("Contract cancelled | company={} | contract={}", companyId, id);

        return toDTO(contractRepository.save(contract));
    }

    /**
     * Applies a price adjustment to a contract.
     *
     * Rules:
     * - Only ACTIVE contracts can be adjusted
     * - Adjustment history must be stored
     * - Financial impact must be updated if value changes
     */
    public ContractAdjustmentResponseDTO adjust(
            UUID contractId,
            ContractAdjustmentRequestDTO dto,
            UUID companyId
    ) {
        Contract contract = findContractOrThrow(contractId, companyId);

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessException("Only active contracts can be adjusted.");
        }

        BigDecimal previousValue = contract.getMonthlyValue();
        BigDecimal newValue = dto.newValue();

        // Store adjustment history for audit purposes
        BigDecimal adjustmentPercent = newValue
                .subtract(previousValue)
                .divide(previousValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        // save history
        ContractAdjustment adjustment = ContractAdjustment.builder()
                .contract(contract)
                .previousValue(previousValue)
                .newValue(newValue)
                .adjustmentPercent(adjustmentPercent)
                .adjustmentDate(LocalDate.now(clock))
                .reason(dto.reason())
                .build();

        contractAdjustmentRepository.save(adjustment);

        // updates contract value
        contract.setMonthlyValue(newValue);
        contractRepository.save(contract);

        // Register financial impact only if there is a difference
        BigDecimal difference = newValue.subtract(previousValue).abs();
        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            registerFinancialImpact(contract, companyId);
        }

        log.info("Contract adjusted | company={} | contract={} | {}% | {} -> {}",
                companyId, contractId, adjustmentPercent, previousValue, newValue);

        return toAdjustmentDTO(adjustment);
    }

    /**
     * Returns all adjustments for a contract.
     */
    @Transactional(readOnly = true)
    public List<ContractAdjustmentResponseDTO> findAdjustments(UUID contractId, UUID companyId) {

        // Ensures contract belongs to the company (security check)
        findContractOrThrow(contractId, companyId); // confirms that the contract belongs to the company
        return contractAdjustmentRepository.findAllByContractId(contractId)
                .stream()
                .map(this::toAdjustmentDTO)
                .toList();
    }

    /**
     * Calculates the financial impact of active contracts.
     *
     * - CLIENT contracts = income
     * - SUPPLIER contracts = expense
     */
    @Transactional(readOnly = true)
    public ContractCashFlowDTO getCashFlowImpact(UUID companyId) {

        BigDecimal monthlyIncome = contractRepository.sumActiveByType(companyId, ContractType.CLIENT);
        BigDecimal monthlyExpense = contractRepository.sumActiveByType(companyId, ContractType.SUPPLIER);
        BigDecimal netImpact = monthlyIncome.subtract(monthlyExpense);

        return new ContractCashFlowDTO(monthlyIncome, monthlyExpense, netImpact);
    }

    /**
     * Registers the financial impact of a contract.
     *
     * Converts contract type into a financial transaction.
     */
    private void registerFinancialImpact(Contract contract, UUID companyId) {

        TransactionType transactionType = contract.getType() == ContractType.CLIENT
                ? TransactionType.INCOME
                : TransactionType.EXPENSE;

        financialService.register(
                new FinancialRequest(
                        "Contract: " + contract.getTitle(),
                        contract.getMonthlyValue(),
                        transactionType,
                        RecurrenceType.FIXED,
                        LocalDateTime.now(clock)
                ),
                companyId
        );
    }

    /**
     * Validates contract dates.
     *
     * Rules:
     * - End date must be after start date
     * - End date cannot be in the past
     */
    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("End date cannot be before start date.");
        }
        if (endDate.isBefore(LocalDate.now(clock))) {
            throw new BusinessException("End date cannot be in the past.");
        }
    }

    /**
     * Retrieves a contract or throws exception if not found or not owned by company.
     */
    private Contract findContractOrThrow(UUID contractId, UUID companyId) {
        return contractRepository.findByIdAndCompanyId(contractId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contract not found: " + contractId));
    }

    private ContractResponseDTO toDTO(Contract contract) {

        LocalDate today = LocalDate.now(clock);
        long daysUntilExpiration = ChronoUnit.DAYS.between(today, contract.getEndDate());

        // Update status to EXPIRED if it has already expired
        ContractStatus status = contract.getStatus();
        if (status == ContractStatus.ACTIVE && contract.getEndDate().isBefore(today)) {
            status = ContractStatus.EXPIRED;
        }

        boolean expirationAlert = status == ContractStatus.ACTIVE &&
                daysUntilExpiration <= contract.getAlertDaysBefore() &&
                daysUntilExpiration >= 0;

        return new ContractResponseDTO(
                contract.getId(),
                contract.getTitle(),
                contract.getDescription(),
                contract.getMonthlyValue(),
                contract.getType(),
                status,
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getAlertDaysBefore(),
                daysUntilExpiration,
                expirationAlert
        );
    }

    private ContractAdjustmentResponseDTO toAdjustmentDTO(ContractAdjustment adjustment) {
        return new ContractAdjustmentResponseDTO(
                adjustment.getId(),
                adjustment.getPreviousValue(),
                adjustment.getNewValue(),
                adjustment.getAdjustmentPercent(),
                adjustment.getAdjustmentDate(),
                adjustment.getReason()
        );
    }
}
