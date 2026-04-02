package com.pmei.contracts.dto;

import com.pmei.contracts.model.ContractStatus;
import com.pmei.contracts.model.ContractType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContractResponseDTO(
        UUID id,
        String title,
        String description,
        BigDecimal monthlyValue,
        ContractType type,
        ContractStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Integer alertDaysBefore,
        long daysUntilExpiration,
        boolean expirationAlert
) {
}
