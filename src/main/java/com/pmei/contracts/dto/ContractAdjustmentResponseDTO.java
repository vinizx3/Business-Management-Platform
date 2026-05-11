package com.pmei.contracts.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContractAdjustmentResponseDTO(
        UUID id,
        BigDecimal previousValue,
        BigDecimal newValue,
        BigDecimal adjustmentPercent,
        LocalDate adjustmentDate,
        String reason
) {
}
