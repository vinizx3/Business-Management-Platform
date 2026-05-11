package com.pmei.sales.dto;

import com.pmei.sales.model.SaleItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SaleRequestDTO {

    @NotNull(message = "Items list is required.")
    @NotEmpty(message = "Sale must have at least one item.")
    @Valid
    private List<SaleItemRequestDTO> items;
}
