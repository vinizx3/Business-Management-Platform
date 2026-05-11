package com.pmei.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemResponseDTO(
        UUID productId,
        String productName,
        Integer quantity,
        BigDecimal price
) {}
