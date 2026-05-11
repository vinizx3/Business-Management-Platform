package com.pmei.financial.controller;

import com.pmei.config.FinancialProperties;
import com.pmei.financial.dto.*;
import com.pmei.financial.service.FinancialAnalyticsService;
import com.pmei.financial.service.FinancialService;
import com.pmei.security.CustomUserPrincipal;
import com.pmei.shared.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST controller responsible for financial operations.
 *
 * Provides endpoints for:
 * - Transaction management
 * - Financial reports
 * - Dashboard and analytics
 */
@Tag(name = "Financial", description = "Financial management and analytics endpoints")
@RestController
@RequestMapping("/financial")
@RequiredArgsConstructor
@Slf4j
public class FinancialController {

    private final FinancialService service;
    private final FinancialAnalyticsService analyticsService;
    private final FinancialProperties properties;

    /**
     * Registers a single financial transaction.
     */
    @Operation(summary = "Register a financial transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/transactions")
    public ResponseEntity<Void> register(
            @RequestBody @Valid FinancialRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        UUID companyId = principal.getCompanyId();

        log.info("Single transaction request | company={}", companyId);

        service.register(request, companyId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Registers multiple financial transactions.
     */
    @Operation(summary = "Register multiple financial transactions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transactions created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/transactions/batch")
    public ResponseEntity<Void> registerBatch(
            @RequestBody @Valid List<@Valid FinancialRequest> requests,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        UUID companyId = principal.getCompanyId();

        log.info("Batch transactions request | company={} | size={}", companyId, requests.size());

        service.registerBatch(requests, companyId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Returns financial dashboard data.
     */
    @Operation(summary = "Get financial dashboard")
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        UUID companyId = principal.getCompanyId();

        log.info("Dashboard requested | company={}", companyId);

        BigDecimal monthlyAverage = analyticsService.calculateMonthlyAverage(companyId);

        FinancialSummaryResponse summary =
                service.generateSummary(companyId, monthlyAverage);

        FinancialInsightsResponse insights = new FinancialInsightsResponse(
                analyticsService.analyzeExpenseIncrease(companyId, properties.getExpenseIncrease()),
                analyticsService.analyzeNegativeProjection(companyId, properties.getProjectionMonths()),
                analyticsService.analyzeCommitment(companyId, properties.getCommitment())
        );

        return ResponseEntity.ok(
                new DashboardResponse(
                        summary.balance(),
                        summary.monthlyProfit(),
                        monthlyAverage,
                        insights
                )
        );
    }

    /**
     * Generates a financial report for a given period.
     */
    @Operation(summary = "Generate financial report")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated"),
            @ApiResponse(responseCode = "400", description = "Invalid date range")
    })
    @GetMapping("/report")
    public ResponseEntity<ReportResponse> getReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        UUID companyId = principal.getCompanyId();

        if (start.isAfter(end)) {
            log.warn("Invalid report period | company={} | start={} | end={}", companyId, start, end);
            throw new BusinessException("Start date cannot be after end date");
        }

        log.info("Report requested | company={} | period={} - {}", companyId, start, end);

        return ResponseEntity.ok(
                service.generateReport(companyId, start, end)
        );
    }
}