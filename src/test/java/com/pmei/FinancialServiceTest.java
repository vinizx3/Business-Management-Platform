package com.pmei;

import com.pmei.company.model.Company;
import com.pmei.company.repository.CompanyRepository;
import com.pmei.financial.dto.FinancialRequest;
import com.pmei.financial.model.FinancialTransaction;
import com.pmei.financial.model.RecurrenceType;
import com.pmei.financial.model.TransactionStatus;
import com.pmei.financial.model.TransactionType;
import com.pmei.financial.repository.FinancialTransactionRepository;
import com.pmei.financial.service.FinancialService;
import com.pmei.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;


import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialServiceTest {

    @Mock
    private FinancialTransactionRepository repository;

    @Mock
    private CompanyRepository companyRepository;

    private Clock fixedClock;

    private FinancialService service;

    private UUID companyId = UUID.randomUUID();
    private Company company;

    @BeforeEach
    void setup() {
        fixedClock = Clock.fixed(
                LocalDate.of(2025, 1, 1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );

        service = new FinancialService(repository, companyRepository, fixedClock);

        company = new Company();
        ReflectionTestUtils.setField(company, "id", companyId);
    }


    @Test
    void shouldRegisterTransactionSuccessfully() {

        FinancialRequest request = new FinancialRequest(
                "Sale",
                new BigDecimal("100.00"),
                TransactionType.INCOME,
                RecurrenceType.NONE,
                LocalDateTime.of(2025, 1, 1, 0, 0)
        );

        when(companyRepository.findById(companyId))
                .thenReturn(Optional.of(company));

        service.register(request, companyId);

        verify(repository, times(1)).save(any(FinancialTransaction.class));
    }

    @Test
    void shouldRegisterAsScheduledWhenDateIsFuture() {

        FinancialRequest request = new FinancialRequest(
                "Future sale",
                new BigDecimal("100.00"),
                TransactionType.INCOME,
                RecurrenceType.NONE,
                LocalDateTime.of(2025, 1, 2, 0, 0)
        );

        when(companyRepository.findById(companyId))
                .thenReturn(Optional.of(company));

        service.register(request, companyId);

        verify(repository, times(1)).save(argThat(transaction ->
                transaction.getStatus() == TransactionStatus.SCHEDULED
        ));
    }

    @Test
    void shouldThrowEntityNotFoundWhenCompanyDoesNotExist() {

        FinancialRequest request = new FinancialRequest(
                "Sale",
                new BigDecimal("100.00"),
                TransactionType.INCOME,
                RecurrenceType.NONE,
                LocalDateTime.of(2025, 1, 1, 0, 0)
        );

        when(companyRepository.findById(companyId))
                .thenReturn(Optional.empty());

        assertThrows(
                BusinessException.class,
                () -> service.register(request, companyId)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldRegisterBatchSuccessfully() {

        FinancialRequest request1 = new FinancialRequest(
                "Sale 1",
                new BigDecimal("100"),
                TransactionType.INCOME,
                RecurrenceType.NONE,
                LocalDateTime.of(2025, 1, 1, 0, 0)
        );

        FinancialRequest request2 = new FinancialRequest(
                "Sale 2",
                new BigDecimal("200"),
                TransactionType.INCOME,
                RecurrenceType.NONE,
                LocalDateTime.of(2025, 1, 1, 0, 0)
        );

        when(companyRepository.findById(companyId))
                .thenReturn(Optional.of(company));

        service.registerBatch(List.of(request1, request2), companyId);

        verify(repository, times(2)).save(any(FinancialTransaction.class));
    }

    @Test
    void shouldThrowBusinessExceptionWhenAmountIsZero() {

        FinancialRequest request = new FinancialRequest(
                "Sale",
                BigDecimal.ZERO,
                TransactionType.INCOME,
                RecurrenceType.NONE,
                LocalDateTime.of(2025, 1, 1, 0, 0)
        );

        assertThrows(BusinessException.class,
                () -> service.register(request, companyId));

        verify(repository, never()).save(any());
    }

    @Test
    void shouldRegisterAsRealizedWhenDateIsPresent() {

        FinancialRequest request = new FinancialRequest(
                "Sale",
                new BigDecimal("100.00"),
                TransactionType.INCOME,
                RecurrenceType.NONE,
                LocalDateTime.of(2025, 1, 1, 0, 0)
        );

        when(companyRepository.findById(companyId))
                .thenReturn(Optional.of(company));

        service.register(request, companyId);

        verify(repository).save(argThat(t ->
                t.getStatus() == TransactionStatus.REALIZED
        ));
    }

    @Test
    void shouldCalculateCorrectBalance() {

        when(repository.sumByCompanyTypeAndStatus(
                companyId, TransactionType.INCOME, TransactionStatus.REALIZED))
                .thenReturn(new BigDecimal("1000"));

        when(repository.sumByCompanyTypeAndStatus(
                companyId, TransactionType.EXPENSE, TransactionStatus.REALIZED))
                .thenReturn(new BigDecimal("300"));

        BigDecimal balance = service.calculateCurrentBalance(companyId);

        assertEquals(new BigDecimal("700"), balance);
    }
}
