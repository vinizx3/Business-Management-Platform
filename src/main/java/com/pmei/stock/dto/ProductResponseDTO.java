package com.pmei.stock.dto;

import com.pmei.stock.model.MovementType;
import com.pmei.stock.model.Product;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.BiConsumer;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {

    private UUID id;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private Integer minimumStock;
}
