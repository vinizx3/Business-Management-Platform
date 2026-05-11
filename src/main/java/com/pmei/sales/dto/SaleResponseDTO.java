package com.pmei.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SaleResponseDTO(
        UUID id,
        BigDecimal totalAmount,
        LocalDateTime date,
        List<SaleItemResponseDTO> items
) {}
