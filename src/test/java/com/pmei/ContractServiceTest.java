package com.pmei;

import com.pmei.company.model.Company;
import com.pmei.company.repository.CompanyRepository;
import com.pmei.contracts.dto.*;
import com.pmei.contracts.model.Contract;
import com.pmei.contracts.model.ContractAdjustment;
import com.pmei.contracts.model.ContractStatus;
import com.pmei.contracts.model.ContractType;
import com.pmei.contracts.repository.ContractAdjustmentRepository;
import com.pmei.contracts.repository.ContractRepository;
import com.pmei.contracts.service.ContractService;
import com.pmei.financial.model.RecurrenceType;
import com.pmei.financial.model.TransactionType;
import com.pmei.financial.service.FinancialService;
import com.pmei.shared.exception.BusinessException;
import com.pmei.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractAdjustmentRepository contractAdjustmentRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private FinancialService financialService;

    private Clock clock;
    private ContractService service;

    private UUID companyId;
    private UUID contractId;
    private Company company;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(
                LocalDate.of(2026, 6, 10)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );

        service = new ContractService(
                contractRepository,
                contractAdjustmentRepository,
                companyRepository,
                financialService,
                clock
        );

        companyId = UUID.randomUUID();
        contractId = UUID.randomUUID();

        company = new Company();
        ReflectionTestUtils.setField(company, "id", companyId);
    }

    // helper to create test contract
    private Contract buildContract(ContractType type, ContractStatus status, LocalDate endDate) {
        Contract contract = new Contract();
        ReflectionTestUtils.setField(contract, "id", contractId);
        contract.setTitle("Test Contract");
        contract.setDescription("Description");
        contract.setMonthlyValue(new BigDecimal("1000.00"));
        contract.setType(type);
        contract.setStatus(status);
        contract.setStartDate(LocalDate.of(2026, 1, 1));
        contract.setEndDate(endDate);
        contract.setAlertDaysBefore(30);
        contract.setCompany(company);
        return contract;
    }

    // create
    @Test
    void shouldCreateClientContractAndRegisterIncome() {

        ContractRequestDTO dto = new ContractRequestDTO(
                "Customer Agreement",
                "Description",
                new BigDecimal("1000.00"),
                ContractType.CLIENT,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                30
        );

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(contractRepository.save(any())).thenAnswer(inv -> {
            Contract c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", contractId);
            return c;
        });

        ContractResponseDTO response = service.create(dto, companyId);

        assertEquals("Customer Agreement", response.title());
        assertEquals(ContractStatus.ACTIVE, response.status());
        assertEquals(ContractType.CLIENT, response.type());

        // The CLIENT must generate an INCOME in the financial module.
        verify(financialService).register(
                argThat(req ->
                        req.type() == TransactionType.INCOME &&
                                req.recurrence() == RecurrenceType.FIXED &&
                                req.amount().compareTo(new BigDecimal("1000.00")) == 0
                ),
                eq(companyId)
        );
    }

    @Test
    void shouldCreateSupplierContractAndRegisterExpense() {

        ContractRequestDTO dto = new ContractRequestDTO(
                "Supplier Contract",
                "Description",
                new BigDecimal("500.00"),
                ContractType.SUPPLIER,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                30
        );

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(contractRepository.save(any())).thenAnswer(inv -> {
            Contract c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", contractId);
            return c;
        });

        service.create(dto, companyId);

        // SUPPLIER must generate EXPENSE in the financial section.
        verify(financialService).register(
                argThat(req -> req.type() == TransactionType.EXPENSE),
                eq(companyId)
        );
    }

    @Test
    void shouldThrowWhenCompanyNotFoundOnCreate() {

        ContractRequestDTO dto = new ContractRequestDTO(
                "Contract",
                "Description",
                new BigDecimal("1000.00"),
                ContractType.CLIENT,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                30
        );

        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(dto, companyId));

        verify(contractRepository, never()).save(any());
        verify(financialService, never()).register(any(), any());
    }

    @Test
    void shouldThrowWhenEndDateBeforeStartDate() {

        ContractRequestDTO dto = new ContractRequestDTO(
                "Contract",
                "Description",
                new BigDecimal("1000.00"),
                ContractType.CLIENT,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 1, 1),
                30
        );

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        assertThrows(BusinessException.class,
                () -> service.create(dto, companyId));

        verify(contractRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEndDateIsInThePast() {

        // endDate in the past
        ContractRequestDTO dto = new ContractRequestDTO(
                "Contract",
                "Description",
                new BigDecimal("1000.00"),
                ContractType.CLIENT,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                30
        );

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        assertThrows(BusinessException.class,
                () -> service.create(dto, companyId));

        verify(contractRepository, never()).save(any());
    }

    // cancel
    @Test
    void shouldCancelActiveContract() {

        Contract contract = buildContract(
                ContractType.CLIENT, ContractStatus.ACTIVE, LocalDate.of(2027, 1, 1));

        when(contractRepository.findByIdAndCompanyId(contractId, companyId))
                .thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenReturn(contract);

        ContractResponseDTO response = service.cancel(contractId, companyId);

        assertEquals(ContractStatus.CANCELLED, response.status());
        verify(contractRepository).save(argThat(c ->
                c.getStatus() == ContractStatus.CANCELLED
        ));
    }

    @Test
    void shouldThrowWhenCancellingAlreadyCancelledContract() {

        Contract contract = buildContract(
                ContractType.CLIENT, ContractStatus.CANCELLED, LocalDate.of(2027, 1, 1));

        when(contractRepository.findByIdAndCompanyId(contractId, companyId))
                .thenReturn(Optional.of(contract));

        assertThrows(BusinessException.class,
                () -> service.cancel(contractId, companyId));

        verify(contractRepository, never()).save(any());
    }

    // adjust
    @Test
    void shouldAdjustContractValueAndSaveHistory() {

        Contract contract = buildContract(
                ContractType.CLIENT, ContractStatus.ACTIVE, LocalDate.of(2027, 1, 1));

        ContractAdjustmentRequestDTO dto = new ContractAdjustmentRequestDTO(
                new BigDecimal("1200.00"),
                "Annual adjustment"
        );

        when(contractRepository.findByIdAndCompanyId(contractId, companyId))
                .thenReturn(Optional.of(contract));
        when(contractAdjustmentRepository.save(any())).thenAnswer(inv -> {
            ContractAdjustment adj = inv.getArgument(0);
            ReflectionTestUtils.setField(adj, "id", UUID.randomUUID());
            return adj;
        });

        ContractAdjustmentResponseDTO response = service.adjust(contractId, dto, companyId);

        // Percentage: (1200 - 1000) / 1000 * 100 = 20%
        assertEquals(new BigDecimal("20.00"), response.adjustmentPercent());
        assertEquals(new BigDecimal("1000.00"), response.previousValue());
        assertEquals(new BigDecimal("1200.00"), response.newValue());
        assertEquals("Annual adjustment", response.reason());

        // The contract should have a new value.
        assertEquals(new BigDecimal("1200.00"), contract.getMonthlyValue());

        // The financial department should be contacted with a new value.
        verify(financialService).register(
                argThat(req -> req.amount().compareTo(new BigDecimal("1200.00")) == 0),
                eq(companyId)
        );
    }

    @Test
    void shouldThrowWhenAdjustingInactiveContract() {

        Contract contract = buildContract(
                ContractType.CLIENT, ContractStatus.CANCELLED, LocalDate.of(2027, 1, 1));

        ContractAdjustmentRequestDTO dto = new ContractAdjustmentRequestDTO(
                new BigDecimal("1200.00"),
                "Readjustment"
        );

        when(contractRepository.findByIdAndCompanyId(contractId, companyId))
                .thenReturn(Optional.of(contract));

        assertThrows(BusinessException.class,
                () -> service.adjust(contractId, dto, companyId));

        verify(contractAdjustmentRepository, never()).save(any());
        verify(financialService, never()).register(any(), any());
    }

    @Test
    void shouldNotRegisterFinancialWhenNewValueEqualsOldValue() {

        Contract contract = buildContract(
                ContractType.CLIENT, ContractStatus.ACTIVE, LocalDate.of(2027, 1, 1));

        // same value — difference = 0
        ContractAdjustmentRequestDTO dto = new ContractAdjustmentRequestDTO(
                new BigDecimal("1000.00"),
                "No change"
        );

        when(contractRepository.findByIdAndCompanyId(contractId, companyId))
                .thenReturn(Optional.of(contract));
        when(contractAdjustmentRepository.save(any())).thenAnswer(inv -> {
            ContractAdjustment adj = inv.getArgument(0);
            ReflectionTestUtils.setField(adj, "id", UUID.randomUUID());
            return adj;
        });

        service.adjust(contractId, dto, companyId);

        // zero difference — finance should NOT be called
        verify(financialService, never()).register(any(), any());
    }

    // toDTO — alerts and automatic status
    @Test
    void shouldReturnExpirationAlertWhenContractIsExpiringSoon() {

        // Clock starts on 2026-06-10, contract expires on 2026-07-01 = 21 days
        // alertDaysBefore = 30 → should alert
        Contract contract = buildContract(
                ContractType.CLIENT, ContractStatus.ACTIVE, LocalDate.of(2026, 7, 1));

        when(contractRepository.findByIdAndCompanyId(contractId, companyId))
                .thenReturn(Optional.of(contract));

        ContractResponseDTO response = service.findById(contractId, companyId);

        assertTrue(response.expirationAlert());
        assertEquals(21, response.daysUntilExpiration());
    }

    @Test
    void shouldNotReturnAlertWhenContractIsNotExpiringSoon() {

        // Clock starts on 2026-06-10, contract expires on 2027-01-01 = 205 days
        // alertDaysBefore = 30 → should not trigger an alert
        Contract contract = buildContract(
                ContractType.CLIENT, ContractStatus.ACTIVE, LocalDate.of(2027, 1, 1));

        when(contractRepository.findByIdAndCompanyId(contractId, companyId))
                .thenReturn(Optional.of(contract));

        ContractResponseDTO response = service.findById(contractId, companyId);

        assertFalse(response.expirationAlert());
    }

    @Test
    void shouldReturnExpiredStatusWhenContractEndDateHasPassed() {

        // clock on 2026-06-10, contract expired on 2026-01-01
        Contract contract = buildContract(
                ContractType.CLIENT, ContractStatus.ACTIVE, LocalDate.of(2026, 1, 1));

        when(contractRepository.findByIdAndCompanyId(contractId, companyId))
                .thenReturn(Optional.of(contract));

        ContractResponseDTO response = service.findById(contractId, companyId);

        assertEquals(ContractStatus.EXPIRED, response.status());
        assertFalse(response.expirationAlert()); // Expired does not generate alert
    }
    // getCashFlowImpact
    @Test
    void shouldCalculateCashFlowImpactCorrectly() {

        when(contractRepository.sumActiveByType(companyId, ContractType.CLIENT))
                .thenReturn(new BigDecimal("5000.00"));

        when(contractRepository.sumActiveByType(companyId, ContractType.SUPPLIER))
                .thenReturn(new BigDecimal("2000.00"));

        ContractCashFlowDTO result = service.getCashFlowImpact(companyId);

        assertEquals(new BigDecimal("5000.00"), result.monthlyIncome());
        assertEquals(new BigDecimal("2000.00"), result.monthlyExpense());
        assertEquals(new BigDecimal("3000.00"), result.netImpact());
    }
}
