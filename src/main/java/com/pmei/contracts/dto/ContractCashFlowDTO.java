package com.pmei.contracts.dto;

import java.math.BigDecimal;

public record ContractCashFlowDTO(
        BigDecimal monthlyIncome,
        BigDecimal monthlyExpense,
        BigDecimal netImpact
) {}
