package com.pmei.stock.dto;

import com.pmei.stock.model.MovementType;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementResponseDTO(
        UUID id,
        UUID productId,
        String productName,
        Integer quantity,
        MovementType type,
        LocalDateTime date
) {}
