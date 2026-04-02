package com.pmei.contracts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ContractAdjustmentRequestDTO(
        @NotNull
        @Positive
        BigDecimal newValue,

        @NotBlank
        String reason
) {
}
