package com.pmei;

import com.pmei.financial.dto.CommitmentInsightDTO;
import com.pmei.financial.dto.ExpenseIncreaseInsightDTO;
import com.pmei.financial.dto.NegativeProjectionInsightDTO;
import com.pmei.financial.model.RecurrenceType;
import com.pmei.financial.model.TransactionStatus;
import com.pmei.financial.model.TransactionType;
import com.pmei.financial.repository.FinancialTransactionRepository;
import com.pmei.financial.service.FinancialAnalyticsService;
import com.pmei.financial.service.FinancialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialAnalyticsServiceTest {

    @Mock
    private FinancialTransactionRepository repository;

    @Mock
    private FinancialService financialService;

    private Clock clock = Clock.fixed(
            LocalDate.of(2026, 6, 10)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant(),
            ZoneId.systemDefault()
    );

    private FinancialAnalyticsService service;

    @BeforeEach
    void setup() {
        service = new FinancialAnalyticsService(repository, financialService, clock);
    }

    @Test
    void shouldReturnZeroAverageWhenNoTransactions() {

        UUID companyId = UUID.randomUUID();

        when(repository.findFirstTransactionDate(companyId)).thenReturn(null);

        BigDecimal result = service.calculateMonthlyAverage(companyId);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void shouldCalculateMonthlyAverageCorrectly() {

        UUID companyId = UUID.randomUUID();

        LocalDateTime firstDate = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime lastDate = LocalDateTime.of(2026, 5, 31, 23, 59);

        when(repository.findFirstTransactionDate(companyId)).thenReturn(firstDate);
        when(repository.findLastTransactionDate(companyId)).thenReturn(lastDate);


        when(repository.sumByCompanyTypeStatusAndPeriod(
                eq(companyId), eq(TransactionType.INCOME), eq(TransactionStatus.REALIZED),
                any(), any())).thenReturn(new BigDecimal("800"));

        when(repository.sumByCompanyTypeStatusAndPeriod(
                eq(companyId), eq(TransactionType.EXPENSE), eq(TransactionStatus.REALIZED),
                any(), any())).thenReturn(new BigDecimal("200"));

        BigDecimal result = service.calculateMonthlyAverage(companyId);

        assertEquals(new BigDecimal("300.00"), result);
    }

    @Test
    void shouldReturnZeroAverageWhenProfitIsZero() {

        UUID companyId = UUID.randomUUID();

        LocalDateTime firstDate = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime lastDate = LocalDateTime.of(2026, 5, 31, 23, 59);

        when(repository.findFirstTransactionDate(companyId)).thenReturn(firstDate);
        when(repository.findLastTransactionDate(companyId)).thenReturn(lastDate);

        when(repository.sumByCompanyTypeStatusAndPeriod(
                eq(companyId), eq(TransactionType.INCOME), eq(TransactionStatus.REALIZED),
                any(), any())).thenReturn(new BigDecimal("500"));

        when(repository.sumByCompanyTypeStatusAndPeriod(
                eq(companyId), eq(TransactionType.EXPENSE), eq(TransactionStatus.REALIZED),
                any(), any())).thenReturn(new BigDecimal("500"));

        BigDecimal result = service.calculateMonthlyAverage(companyId);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void shouldReturnAlertWhenExpensesIncreaseAboveThreshold() {

        UUID companyId = UUID.randomUUID();
        LocalDateTime lastDate = LocalDateTime.of(2026, 5, 15, 0, 0);

        when(repository.findLastTransactionDate(companyId)).thenReturn(lastDate);

        when(repository.sumByCompanyTypeStatusAndPeriod(
                eq(companyId), eq(TransactionType.EXPENSE), eq(TransactionStatus.REALIZED),
                any(), any()))
                .thenReturn(new BigDecimal("1500"))
                .thenReturn(new BigDecimal("1000"));

        ExpenseIncreaseInsightDTO result =
                service.analyzeExpenseIncrease(companyId, BigDecimal.valueOf(20));

        assertTrue(result.alert());
        assertEquals(new BigDecimal("50.00"), result.increasePercent());
    }

    @Test
    void shouldNotReturnAlertWhenExpensesIncreaseBelowThreshold() {

        UUID companyId = UUID.randomUUID();
        LocalDateTime lastDate = LocalDateTime.of(2026, 5, 15, 0, 0);

        when(repository.findLastTransactionDate(companyId)).thenReturn(lastDate);

        when(repository.sumByCompanyTypeStatusAndPeriod(
                eq(companyId), eq(TransactionType.EXPENSE), eq(TransactionStatus.REALIZED),
                any(), any()))
                .thenReturn(new BigDecimal("1100"))
                .thenReturn(new BigDecimal("1000"));

        ExpenseIncreaseInsightDTO result =
                service.analyzeExpenseIncrease(companyId, BigDecimal.valueOf(20));

        assertFalse(result.alert());
    }

    @Test
    void shouldReturnEmptyInsightWhenNoTransactions() {

        UUID companyId = UUID.randomUUID();

        when(repository.findLastTransactionDate(companyId)).thenReturn(null);

        ExpenseIncreaseInsightDTO result =
                service.analyzeExpenseIncrease(companyId, BigDecimal.valueOf(20));

        assertFalse(result.alert());
        assertEquals(BigDecimal.ZERO, result.currentMonthExpenses());
    }

    @Test
    void shouldAlertWhenProjectedBalanceIsNegative() {

        UUID companyId = UUID.randomUUID();

        when(financialService.calculateCurrentBalance(companyId))
                .thenReturn(new BigDecimal("1000"));

        when(repository.findFirstTransactionDate(companyId)).thenReturn(null);

        when(repository.sumScheduledByTypeAndPeriod(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        when(repository.sumScheduledByTypeAndPeriod(
                eq(companyId), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("5000"));

        NegativeProjectionInsightDTO result =
                service.analyzeNegativeProjection(companyId, 3);

        assertTrue(result.alert());
        assertTrue(result.projectedBalance().compareTo(BigDecimal.ZERO) < 0);
    }

    @Test
    void shouldNotAlertWhenProjectedBalanceIsPositive() {

        UUID companyId = UUID.randomUUID();

        when(financialService.calculateCurrentBalance(companyId))
                .thenReturn(new BigDecimal("10000"));

        when(repository.findFirstTransactionDate(companyId)).thenReturn(null);

        when(repository.sumScheduledByTypeAndPeriod(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        NegativeProjectionInsightDTO result =
                service.analyzeNegativeProjection(companyId, 3);

        assertFalse(result.alert());
    }

    @Test
    void shouldAlertWhenCommitmentExceedsThreshold() {

        UUID companyId = UUID.randomUUID();
        LocalDateTime lastDate = LocalDateTime.of(2026, 5, 15, 0, 0);

        when(repository.findLastTransactionDate(companyId)).thenReturn(lastDate);

        when(repository.sumByCompanyTypeStatusAndPeriod(
                eq(companyId), eq(TransactionType.INCOME), eq(TransactionStatus.REALIZED),
                any(), any())).thenReturn(new BigDecimal("1000"));

        when(repository.sumExpensesByRecurrenceAndStatusAndPeriod(
                eq(companyId), eq(RecurrenceType.FIXED), eq(TransactionStatus.REALIZED),
                any(), any())).thenReturn(new BigDecimal("800"));

        CommitmentInsightDTO result =
                service.analyzeCommitment(companyId, BigDecimal.valueOf(70));

        assertTrue(result.alert());
        assertEquals(new BigDecimal("80.00"), result.commitmentPercent());
    }

    @Test
    void shouldNotAlertWhenCommitmentIsBelowThreshold() {

        UUID companyId = UUID.randomUUID();
        LocalDateTime lastDate = LocalDateTime.of(2026, 5, 15, 0, 0);

        when(repository.findLastTransactionDate(companyId)).thenReturn(lastDate);

        when(repository.sumByCompanyTypeStatusAndPeriod(
                eq(companyId), eq(TransactionType.INCOME), eq(TransactionStatus.REALIZED),
                any(), any())).thenReturn(new BigDecimal("1000"));

        when(repository.sumExpensesByRecurrenceAndStatusAndPeriod(
                eq(companyId), eq(RecurrenceType.FIXED), eq(TransactionStatus.REALIZED),
                any(), any())).thenReturn(new BigDecimal("500"));

        CommitmentInsightDTO result =
                service.analyzeCommitment(companyId, BigDecimal.valueOf(70));

        assertFalse(result.alert());
        assertEquals(new BigDecimal("50.00"), result.commitmentPercent());
    }
}
