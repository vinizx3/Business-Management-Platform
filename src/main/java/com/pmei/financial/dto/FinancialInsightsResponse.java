package com.pmei.financial.dto;


public record FinancialInsightsResponse(
        ExpenseIncreaseInsightDTO expenseIncrease,
        NegativeProjectionInsightDTO negativeProjection,
        CommitmentInsightDTO commitment
) {
}
