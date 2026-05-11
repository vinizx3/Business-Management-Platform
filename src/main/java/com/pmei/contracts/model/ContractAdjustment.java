package com.pmei.contracts.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contract_adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(nullable = false)
    private BigDecimal previousValue;

    @Column(nullable = false)
    private BigDecimal newValue;

    @Column(nullable = false)
    private BigDecimal adjustmentPercent;

    @Column(nullable = false)
    private LocalDate adjustmentDate;

    @Column(length = 300)
    private String reason;
}
