package com.pmei.financial.repository;

import com.pmei.financial.dto.FinancialAggregateDTO;
import com.pmei.financial.model.FinancialTransaction;
import com.pmei.financial.model.TransactionStatus;
import com.pmei.financial.model.TransactionType;
import com.pmei.financial.model.RecurrenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repository responsible for FinancialTransaction data access.
 *
 * Provides complex queries for:
 * - Financial summaries
 * - Time-based analysis
 * - Projections and insights
 */
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, UUID> {


    /**
     * Returns the date of the first transaction of a company.
     */
    @Query("""
        SELECT MIN(t.date)
        FROM FinancialTransaction t
        WHERE t.company.id = :companyId
    """)
    LocalDateTime findFirstTransactionDate(UUID companyId);
    /**
        * Returns the date of the most recent transaction of a company.
     */
    @Query("""
        SELECT MAX(t.date)
        FROM FinancialTransaction t
        WHERE t.company.id = :companyId
    """)
    LocalDateTime findLastTransactionDate(UUID companyId);

    /**
     * Sums transactions by type and status.
     */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM FinancialTransaction t
        WHERE t.company.id = :companyId
        AND t.type = :type
        AND t.status = :status
    """)
    BigDecimal sumByCompanyTypeAndStatus(
            UUID companyId,
            TransactionType type,
            TransactionStatus status
    );

    /**
     * Sums transactions by type, status, and period.
     */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM FinancialTransaction t
        WHERE t.company.id = :companyId
        AND t.type = :type
        AND t.status = :status
        AND t.date BETWEEN :startDate AND :endDate
    """)
    BigDecimal sumByCompanyTypeStatusAndPeriod(
            UUID companyId,
            TransactionType type,
            TransactionStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Sums scheduled (future) transactions within a period.
     */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM FinancialTransaction t
        WHERE t.company.id = :companyId
        AND t.type = :type
        AND t.status = 'SCHEDULED'
        AND t.date BETWEEN :startDate AND :endDate
    """)
    BigDecimal sumScheduledByTypeAndPeriod(
            UUID companyId,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Sums fixed expenses within a period.
     */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM FinancialTransaction t
        WHERE t.company.id = :companyId
        AND t.type = 'EXPENSE'
        AND t.recurrence = :recurrence
        AND t.status = :status
        AND t.date BETWEEN :start AND :end
    """)
    BigDecimal sumExpensesByRecurrenceAndStatusAndPeriod(
            UUID companyId,
            RecurrenceType recurrence,
            TransactionStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Aggregates financial data for dashboard summary.
     */
    @Query("""
    SELECT new com.pmei.financial.dto.FinancialAggregateDTO(
        COALESCE(SUM(CASE 
            WHEN t.type = 'INCOME' AND t.status = 'REALIZED' 
            THEN t.amount ELSE 0 END), 0),
        COALESCE(SUM(CASE 
            WHEN t.type = 'EXPENSE' AND t.status = 'REALIZED' 
            THEN t.amount ELSE 0 END), 0),
        COALESCE(SUM(CASE 
            WHEN t.type = 'INCOME' 
            AND t.status = 'REALIZED'
            AND t.date BETWEEN :start AND :end 
            THEN t.amount ELSE 0 END), 0),
        COALESCE(SUM(CASE 
            WHEN t.type = 'EXPENSE' 
            AND t.status = 'REALIZED'
            AND t.date BETWEEN :start AND :end 
            THEN t.amount ELSE 0 END), 0)
    )
    FROM FinancialTransaction t
    WHERE t.company.id = :companyId
""")
    FinancialAggregateDTO aggregateSummary(
            UUID companyId,
            LocalDateTime start,
            LocalDateTime end
    );
}