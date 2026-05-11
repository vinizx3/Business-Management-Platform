package com.pmei.financial.service;

import com.pmei.company.model.Company;
import com.pmei.company.repository.CompanyRepository;
import com.pmei.financial.dto.FinancialRequest;
import com.pmei.financial.dto.FinancialSummaryResponse;
import com.pmei.financial.dto.ReportResponse;
import com.pmei.financial.model.FinancialTransaction;
import com.pmei.financial.model.TransactionStatus;
import com.pmei.financial.model.TransactionType;
import com.pmei.financial.repository.FinancialTransactionRepository;
import com.pmei.shared.exception.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for handling financial transactions and summaries.
 *
 * This service manages:
 * - Transaction registration (single and batch)
 * - Financial summaries and reports
 * - Balance calculations
 *
 * It ensures validation and consistency of financial data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FinancialService {

    private final FinancialTransactionRepository repository;
    private final CompanyRepository companyRepository;
    private final Clock clock;

    /**
     * Registers a financial transaction.
     *
     * Rules:
     * - Amount must be greater than zero
     * - Company must exist
     * - Transaction status is determined by its date:
     *   - Future → SCHEDULED
     *   - Past/Present → REALIZED
     */
    public void register(FinancialRequest request, UUID companyId) {

        // Validate amount
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid amount for company {}: {}", companyId, request.amount());
            throw new BusinessException("Amount must be greater than zero");
        }

        // Ensure company exists
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> {
                    log.warn("Company not found: {}", companyId);
                    return new BusinessException("Company not found");
                });

        // Determine transaction status based on date
        TransactionStatus status = request.date().isAfter(LocalDateTime.now(clock))
                ? TransactionStatus.SCHEDULED
                : TransactionStatus.REALIZED;

        FinancialTransaction transaction = FinancialTransaction.builder()
                .description(request.description())
                .amount(request.amount())
                .type(request.type())
                .recurrence(request.recurrence())
                .date(request.date())
                .status(status)
                .company(company)
                .build();

        repository.save(transaction);

        log.info("Transaction registered | company={} | type={} | amount={} | status={}",
                companyId, request.type(), request.amount(), status);
    }

    /**
     * Registers multiple transactions in batch.
     *
     * Each transaction is validated individually.
     */
    public void registerBatch(List<FinancialRequest> requests, UUID companyId) {

        log.info("Registering batch transactions | company={} | size={}", companyId, requests.size());

        for (FinancialRequest request : requests) {
            register(request, companyId);
        }
    }

    /**
     * Generates a financial summary for the current month.
     *
     * Includes:
     * - Total balance (income - expense)
     * - Monthly profit
     * - External monthly average (from analytics)
     */
    public FinancialSummaryResponse generateSummary(UUID companyId, BigDecimal monthlyAverage) {

        YearMonth now = YearMonth.now(clock);
        LocalDateTime start = now.atDay(1).atStartOfDay();
        LocalDateTime end = now.atEndOfMonth().atTime(23, 59, 59);

        log.info("Generating financial summary | company={} | period={} - {}", companyId, start, end);

        var aggregate = repository.aggregateSummary(companyId, start, end);

        // Ensure null-safe values
        BigDecimal totalIncome = defaultZero(aggregate.totalIncome());
        BigDecimal totalExpense = defaultZero(aggregate.totalExpense());
        BigDecimal monthlyIncome = defaultZero(aggregate.monthlyIncome());
        BigDecimal monthlyExpense = defaultZero(aggregate.monthlyExpense());

        BigDecimal balance = totalIncome.subtract(totalExpense);
        BigDecimal monthlyProfit = monthlyIncome.subtract(monthlyExpense);

        return new FinancialSummaryResponse(
                balance,
                monthlyProfit,
                monthlyAverage
        );
    }

    /**
     * Generates a financial report for a given period.
     *
     * Only REALIZED transactions are considered.
     */
    public ReportResponse generateReport(UUID companyId, LocalDate start, LocalDate end) {

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        BigDecimal income = defaultZero(
                repository.sumByCompanyTypeStatusAndPeriod(
                        companyId,
                        TransactionType.INCOME,
                        TransactionStatus.REALIZED,
                        startDateTime,
                        endDateTime
                )
        );

        BigDecimal expense = defaultZero(
                repository.sumByCompanyTypeStatusAndPeriod(
                        companyId,
                        TransactionType.EXPENSE,
                        TransactionStatus.REALIZED,
                        startDateTime,
                        endDateTime
                )
        );

        return new ReportResponse(
                start,
                end,
                income,
                expense,
                income.subtract(expense)
        );
    }

    /**
     * Calculates the current balance based on all realized transactions.
     */
    public BigDecimal calculateCurrentBalance(UUID companyId) {

        BigDecimal totalIncome = defaultZero(
                repository.sumByCompanyTypeAndStatus(
                        companyId,
                        TransactionType.INCOME,
                        TransactionStatus.REALIZED
                )
        );

        BigDecimal totalExpense = defaultZero(
                repository.sumByCompanyTypeAndStatus(
                        companyId,
                        TransactionType.EXPENSE,
                        TransactionStatus.REALIZED
                )
        );

        BigDecimal balance = totalIncome.subtract(totalExpense);

        log.info("Current balance calculated | company={} | balance={}", companyId, balance);

        return balance;
    }

    /**
     * Utility method to prevent null values in calculations.
     */
     private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}