package com.pmei.stock.dto;

import com.pmei.stock.model.MovementType;
import com.pmei.stock.model.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDTO{

    @NotBlank(message = "Name is required.")
    String name;

    @NotNull(message = "Price is required.")
    @Positive(message = "Price must be greater than zero.")
    BigDecimal price;

    @NotNull(message = "Quantity is required.")
    @PositiveOrZero(message = "Quantity cannot be negative.")
    Integer quantity;

    @NotNull(message = "Minimum stock is required.")
    @PositiveOrZero(message = "Minimum stock cannot be negative.")
    Integer minimumStock;

//    @NotNull(message = "Supplier id is required.")
//    UUID supplierId;
}


