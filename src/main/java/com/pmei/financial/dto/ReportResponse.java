package com.pmei.financial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportResponse(
        LocalDate start,
        LocalDate end,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netProfit
) {}
