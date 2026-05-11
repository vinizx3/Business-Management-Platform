package com.pmei.financial.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        BigDecimal balance,
        BigDecimal monthlyProfit,
        BigDecimal monthlyAverage,
        FinancialInsightsResponse insights
){
}
