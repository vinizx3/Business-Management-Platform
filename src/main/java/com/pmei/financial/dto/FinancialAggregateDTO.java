package com.pmei.financial.dto;

import java.math.BigDecimal;

public record FinancialAggregateDTO(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal monthlyIncome,
        BigDecimal monthlyExpense
) {
}
