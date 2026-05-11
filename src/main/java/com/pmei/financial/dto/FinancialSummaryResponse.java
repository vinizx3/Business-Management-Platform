package com.pmei.financial.dto;

import java.math.BigDecimal;

public record FinancialSummaryResponse(
        BigDecimal balance,
        BigDecimal monthlyProfit,
        BigDecimal monthlyAverage
) {
}
