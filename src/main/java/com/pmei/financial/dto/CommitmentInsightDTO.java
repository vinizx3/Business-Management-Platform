package com.pmei.financial.dto;

import java.math.BigDecimal;

public record CommitmentInsightDTO(
        BigDecimal totalIncome,
        BigDecimal fixedExpenses,
        BigDecimal commitmentPercent,
        BigDecimal thresholdPercent,
        boolean alert
) {
}
