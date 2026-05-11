package com.pmei.financial.service;

import com.pmei.financial.dto.*;
import com.pmei.financial.model.RecurrenceType;
import com.pmei.financial.model.TransactionStatus;
import com.pmei.financial.model.TransactionType;
import com.pmei.financial.repository.FinancialTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service responsible for financial analytics and insights.
 *
 * Provides:
 * - Monthly average profit calculation
 * - Future balance projections
 * - Expense increase analysis
 * - Financial commitment analysis
 * - Negative balance prediction
 *
 * Focuses on business intelligence and decision support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialAnalyticsService {

    private final FinancialTransactionRepository repository;
    private final FinancialService financialService;
    private final Clock clock;

    /**
     * Calculates the average monthly profit across all historical data.
     *
     * Rule:
     * - Only considers the period between first and last transaction
     */
    public BigDecimal calculateMonthlyAverage(UUID companyId) {

        LocalDateTime firstDate = repository.findFirstTransactionDate(companyId);
        YearMonth lastMonth = getLastMonthWithTransactions(companyId);

        if (firstDate == null || lastMonth == null) {
            return BigDecimal.ZERO;
        }

        YearMonth firstMonth = YearMonth.from(firstDate);

        long months = ChronoUnit.MONTHS.between(firstMonth, lastMonth) + 1;

        if (months <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalProfit = calculateTotalProfit(
                companyId,
                firstMonth.atDay(1),
                lastMonth.atEndOfMonth()
        );

        if (totalProfit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalProfit.divide(
                BigDecimal.valueOf(months),
                2,
                RoundingMode.HALF_UP
        );
    }

    /**
     * Projects future balance based on:
     * - Current balance
     * - Monthly average
     * - Scheduled transactions
     */
    public BigDecimal projectFutureBalance(UUID companyId, int monthsAhead) {

        BigDecimal currentBalance = financialService.calculateCurrentBalance(companyId);
        BigDecimal monthlyAverage = calculateMonthlyAverage(companyId);

        LocalDate today = LocalDate.now(clock);
        LocalDate futureDate = today.plusMonths(monthsAhead);

        BigDecimal scheduledIncome = defaultZero(
                repository.sumScheduledByTypeAndPeriod(
                        companyId,
                        TransactionType.INCOME,
                        startOfDay(today),
                        endOfDay(futureDate)
                )
        );

        BigDecimal scheduledExpense = defaultZero(
                repository.sumScheduledByTypeAndPeriod(
                        companyId,
                        TransactionType.EXPENSE,
                        startOfDay(today),
                        endOfDay(futureDate)
                )
        );

        return currentBalance
                .add(monthlyAverage.multiply(BigDecimal.valueOf(monthsAhead)))
                .add(scheduledIncome)
                .subtract(scheduledExpense);
    }

    /**
     * Detects abnormal increase in expenses compared to previous month.
     */
    public ExpenseIncreaseInsightDTO analyzeExpenseIncrease(
            UUID companyId,
            BigDecimal thresholdPercent
    ) {

        YearMonth lastMonth = getLastMonthWithTransactions(companyId);
        if (lastMonth == null) {
            return new ExpenseIncreaseInsightDTO(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, thresholdPercent, false
            );
        }

        YearMonth previousMonth = lastMonth.minusMonths(1);

        BigDecimal current = sumExpenses(companyId, lastMonth);
        BigDecimal last = sumExpenses(companyId, previousMonth);

        BigDecimal increasePercent = BigDecimal.ZERO;
        boolean alert = false;

        if (last.compareTo(BigDecimal.ZERO) == 0) {
            if (current.compareTo(BigDecimal.ZERO) > 0) {
                increasePercent = BigDecimal.valueOf(100);
                alert = true;
            }
        } else {
            increasePercent = current
                    .subtract(last)
                    .divide(last, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            alert = increasePercent.compareTo(thresholdPercent) > 0;
        }

        if (alert) {
            log.warn("Expense increase alert | company={} | increase={}%", companyId, increasePercent);
        }

        return new ExpenseIncreaseInsightDTO(
                current,
                last,
                increasePercent.setScale(2, RoundingMode.HALF_UP),
                thresholdPercent,
                alert
        );
    }

    /**
     * Measures how much of income is committed to fixed expenses.
     */
    public CommitmentInsightDTO analyzeCommitment(
            UUID companyId,
            BigDecimal thresholdPercent
    ) {

        YearMonth lastMonth = getLastMonthWithTransactions(companyId);
        if (lastMonth == null) {
            return new CommitmentInsightDTO(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, thresholdPercent, false
            );
        }

        LocalDate startDate = lastMonth.atDay(1);
        LocalDate endDate = lastMonth.atEndOfMonth();

        BigDecimal income = defaultZero(
                repository.sumByCompanyTypeStatusAndPeriod(
                        companyId,
                        TransactionType.INCOME,
                        TransactionStatus.REALIZED,
                        startOfDay(startDate),
                        endOfDay(endDate)
                )
        );

        BigDecimal fixedExpenses = defaultZero(
                repository.sumExpensesByRecurrenceAndStatusAndPeriod(
                        companyId,
                        RecurrenceType.FIXED,
                        TransactionStatus.REALIZED,
                        startOfDay(startDate),
                        endOfDay(endDate)
                )
        );

        BigDecimal commitment = BigDecimal.ZERO;

        if (income.compareTo(BigDecimal.ZERO) > 0) {
            commitment = fixedExpenses
                    .divide(income, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        boolean alert = commitment.compareTo(thresholdPercent) > 0;

        if (alert) {
            log.warn("High financial commitment | company={} | commitment={}%", companyId, commitment);
        }

        return new CommitmentInsightDTO(
                income,
                fixedExpenses,
                commitment.setScale(2, RoundingMode.HALF_UP),
                thresholdPercent,
                alert
        );
    }

    /**
     * Analyzes whether the company is projected to have a negative balance
     * in the future based on current data.
     *
     * Considers:
     * - Current balance
     * - Historical monthly average
     * - Future projection (including scheduled transactions)
     *
     * Triggers an alert if projected balance is negative.
     */
    public NegativeProjectionInsightDTO analyzeNegativeProjection(
            UUID companyId,
            int monthsProjection
    ) {

        BigDecimal currentBalance = financialService.calculateCurrentBalance(companyId);
        BigDecimal monthlyAverage = calculateMonthlyAverage(companyId);

        // Calculates projected balance for the given period
        BigDecimal projectedBalance =
                projectFutureBalance(companyId, monthsProjection);

        boolean alert = projectedBalance.compareTo(BigDecimal.ZERO) < 0;

        if (alert) {
            log.warn("Negative projection detected | company={} | projectedBalance={}",
                    companyId, projectedBalance);
        }

        return new NegativeProjectionInsightDTO(
                currentBalance,
                monthlyAverage,
                monthsProjection,
                projectedBalance,
                alert
        );
    }

    // HELPERS

    /**
     * Retrieves the last month in which the company had transactions.
     *
     * Returns null if no transactions exist.
     */
    private YearMonth getLastMonthWithTransactions(UUID companyId) {
        LocalDateTime lastDate = repository.findLastTransactionDate(companyId);
        if (lastDate == null) return null;
        return YearMonth.from(lastDate);
    }

    /**
     * Calculates total profit (income - expense) within a given period.
     *
     * Rules:
     * - Only REALIZED transactions are considered
     * - Future dates are capped to today's date
     */
    private BigDecimal calculateTotalProfit(
            UUID companyId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        LocalDate today = LocalDate.now(clock);

        // Prevent future dates from affecting historical calculations
        if (endDate.isAfter(today)) {
            endDate = today;
        }

        LocalDateTime start = startOfDay(startDate);
        LocalDateTime end = endOfDay(endDate);

        BigDecimal totalIncome = defaultZero(
                repository.sumByCompanyTypeStatusAndPeriod(
                        companyId,
                        TransactionType.INCOME,
                        TransactionStatus.REALIZED,
                        start,
                        end
                )
        );

        BigDecimal totalExpense = defaultZero(
                repository.sumByCompanyTypeStatusAndPeriod(
                        companyId,
                        TransactionType.EXPENSE,
                        TransactionStatus.REALIZED,
                        start,
                        end
                )
        );

        return totalIncome.subtract(totalExpense);
    }

    /**
     * Sums all expenses for a given month.
     *
     * Only REALIZED transactions are considered.
     */
    private BigDecimal sumExpenses(UUID companyId, YearMonth month) {

        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        return defaultZero(
                repository.sumByCompanyTypeStatusAndPeriod(
                        companyId,
                        TransactionType.EXPENSE,
                        TransactionStatus.REALIZED,
                        startOfDay(startDate),
                        endOfDay(endDate)
                )
        );
    }

    /**
     * Converts a LocalDate to the start of the day (00:00:00).
     */
    private LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    /**
     * Converts a LocalDate to the end of the day (23:59:59).
     */
    private LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(23, 59, 59);
    }

    /**
     * Utility method to replace null values with zero in calculations.
     */
    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}