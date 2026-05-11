package com.pmei.financial.dto;

import java.math.BigDecimal;

public record NegativeProjectionInsightDTO(
        BigDecimal currentBalance,
        BigDecimal monthlyAverage,
        int monthsProjection,
        BigDecimal projectedBalance,
        boolean alert
) {
}
