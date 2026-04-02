package com.pmei.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "financial.threshold")
@Getter
@Setter
public class FinancialProperties {

    private BigDecimal expenseIncrease;
    private BigDecimal commitment;
    private int projectionMonths;
}