package com.pmei.financial.dto;

import com.pmei.financial.model.RecurrenceType;
import com.pmei.financial.model.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FinancialRequest(

        @NotBlank(message = "Description is required")
        @Size(max = 150)
        String description,

        @NotNull(message = "Amount is required")
        @Positive
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Transaction type is required")
        TransactionType type,

        @NotNull(message = "Recurrence type is required")
        RecurrenceType recurrence,

        @NotNull(message = "Date is required")
        LocalDateTime date
) {
}
