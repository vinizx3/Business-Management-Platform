package com.pmei.stock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockAdjustmentRequest(
        @NotNull @Positive Integer quantity
) {}
