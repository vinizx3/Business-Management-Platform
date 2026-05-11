package com.pmei.contracts.dto;

import com.pmei.contracts.model.ContractType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractRequestDTO(
        @NotBlank
        String title,

        @Size(max = 500)
        String description,

        @NotNull
        @Positive
        BigDecimal monthlyValue,

        @NotNull
        ContractType type,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate,

        @NotNull
        @Positive
        Integer alertDaysBefore
) {
}
