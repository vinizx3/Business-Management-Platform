package com.pmei.financial.dto;

import java.math.BigDecimal;

public record ExpenseIncreaseInsightDTO(
        BigDecimal currentMonthExpenses,
        BigDecimal lastMonthExpenses,
        BigDecimal increasePercent,
        BigDecimal thresholdPercent,
        boolean alert

) {
}
