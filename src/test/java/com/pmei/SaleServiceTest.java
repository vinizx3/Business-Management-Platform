package com.pmei;

import com.pmei.company.model.Company;
import com.pmei.company.repository.CompanyRepository;
import com.pmei.financial.dto.FinancialRequest;
import com.pmei.financial.model.RecurrenceType;
import com.pmei.financial.model.TransactionType;
import com.pmei.financial.service.FinancialService;
import com.pmei.sales.dto.SaleItemRequestDTO;
import com.pmei.sales.dto.SaleRequestDTO;
import com.pmei.sales.dto.SaleResponseDTO;
import com.pmei.sales.model.Sale;
import com.pmei.sales.model.SaleItem;
import com.pmei.sales.repository.SaleRepository;
import com.pmei.sales.service.SaleService;
import com.pmei.shared.exception.BusinessException;
import com.pmei.shared.exception.ResourceNotFoundException;
import com.pmei.stock.model.Product;
import com.pmei.stock.repository.ProductRepository;
import com.pmei.stock.service.StockService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private StockService stockService;

    @Mock
    private FinancialService financialService;

    private Clock clock;
    private SaleService service;

    private UUID companyId;
    private Company company;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(
                LocalDate.of(2026, 1, 1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );

        service = new SaleService(
                saleRepository,
                companyRepository,
                stockService,
                financialService,
                clock
        );

        companyId = UUID.randomUUID();
        company = new Company();
        ReflectionTestUtils.setField(company, "id", companyId);
    }

    @Test
    void shouldCreateSaleSuccessfully() {

        UUID productId = UUID.randomUUID();

        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", productId);
        product.setName("Notebook");
        product.setPrice(new BigDecimal("2000.00"));
        product.setQuantity(10);

        SaleItemRequestDTO itemDTO = new SaleItemRequestDTO();
        itemDTO.setProductId(productId);
        itemDTO.setQuantity(2);

        SaleRequestDTO request = new SaleRequestDTO();
        request.setItems(List.of(itemDTO));

        when(companyRepository.findById(companyId))
                .thenReturn(Optional.of(company));

        when(stockService.removeStockAndReturn(productId, 2, companyId))
                .thenReturn(product);

        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale sale = invocation.getArgument(0);
            ReflectionTestUtils.setField(sale, "id", UUID.randomUUID());
            return sale;
        });

        SaleResponseDTO response = service.createSale(request, companyId);

        assertEquals(new BigDecimal("4000.00"), response.totalAmount());
        assertEquals(1, response.items().size());
        assertEquals("Notebook", response.items().get(0).productName());

        verify(stockService).removeStockAndReturn(productId, 2, companyId);
        verify(financialService).register(any(FinancialRequest.class), eq(companyId));
        verify(saleRepository).save(any(Sale.class));
    }

    @Test
    void shouldCalculateTotalCorrectlyForMultipleItems() {

        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        Product product1 = new Product();
        ReflectionTestUtils.setField(product1, "id", productId1);
        product1.setName("Mouse");
        product1.setPrice(new BigDecimal("100.00"));
        product1.setQuantity(5);

        Product product2 = new Product();
        ReflectionTestUtils.setField(product2, "id", productId2);
        product2.setName("Keyboard");
        product2.setPrice(new BigDecimal("200.00"));
        product2.setQuantity(3);

        SaleItemRequestDTO item1 = new SaleItemRequestDTO();
        item1.setProductId(productId1);
        item1.setQuantity(3);

        SaleItemRequestDTO item2 = new SaleItemRequestDTO();
        item2.setProductId(productId2);
        item2.setQuantity(2);

        SaleRequestDTO request = new SaleRequestDTO();
        request.setItems(List.of(item1, item2));

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(stockService.removeStockAndReturn(productId1, 3, companyId)).thenReturn(product1);
        when(stockService.removeStockAndReturn(productId2, 2, companyId)).thenReturn(product2);

        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale sale = invocation.getArgument(0);
            ReflectionTestUtils.setField(sale, "id", UUID.randomUUID());
            return sale;
        });

        SaleResponseDTO response = service.createSale(request, companyId);

        assertEquals(new BigDecimal("700.00"), response.totalAmount());
        assertEquals(2, response.items().size());

        verify(stockService).removeStockAndReturn(productId1, 3, companyId);
        verify(stockService).removeStockAndReturn(productId2, 2, companyId);
        verify(financialService).register(any(FinancialRequest.class), eq(companyId));
    }

    @Test
    void shouldThrowWhenCompanyNotFound() {

        SaleItemRequestDTO itemDTO = new SaleItemRequestDTO();
        itemDTO.setProductId(UUID.randomUUID());
        itemDTO.setQuantity(1);

        SaleRequestDTO request = new SaleRequestDTO();
        request.setItems(List.of(itemDTO));

        when(companyRepository.findById(companyId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createSale(request, companyId));

        verify(stockService, never()).removeStockAndReturn(any(), any(), any());
        verify(financialService, never()).register(any(), any());
        verify(saleRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenInsufficientStock() {

        UUID productId = UUID.randomUUID();

        SaleItemRequestDTO itemDTO = new SaleItemRequestDTO();
        itemDTO.setProductId(productId);
        itemDTO.setQuantity(99);

        SaleRequestDTO request = new SaleRequestDTO();
        request.setItems(List.of(itemDTO));

        when(companyRepository.findById(companyId))
                .thenReturn(Optional.of(company));

        when(stockService.removeStockAndReturn(productId, 99, companyId))
                .thenThrow(new BusinessException("Insufficient stock for product"));

        assertThrows(BusinessException.class,
                () -> service.createSale(request, companyId));

        verify(financialService, never()).register(any(), any());
        verify(saleRepository, never()).save(any());
    }

    @Test
    void shouldRegisterFinancialIncomeAfterSale() {

        UUID productId = UUID.randomUUID();

        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", productId);
        product.setName("Product X");
        product.setPrice(new BigDecimal("500.00"));
        product.setQuantity(5);

        SaleItemRequestDTO itemDTO = new SaleItemRequestDTO();
        itemDTO.setProductId(productId);
        itemDTO.setQuantity(1);

        SaleRequestDTO request = new SaleRequestDTO();
        request.setItems(List.of(itemDTO));

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(stockService.removeStockAndReturn(productId, 1, companyId)).thenReturn(product);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale sale = invocation.getArgument(0);
            ReflectionTestUtils.setField(sale, "id", UUID.randomUUID());
            return sale;
        });

        service.createSale(request, companyId);

        verify(financialService).register(
                argThat(req ->
                        req.type() == TransactionType.INCOME &&
                                req.amount().compareTo(new BigDecimal("500.00")) == 0 &&
                                req.recurrence() == RecurrenceType.VARIABLE
                ),
                eq(companyId)
        );
    }

    @Test
    void shouldFindSaleByIdSuccessfully() {

        UUID saleId = UUID.randomUUID();

        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        product.setName("Product");
        product.setPrice(new BigDecimal("100.00"));

        SaleItem item = new SaleItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setPrice(new BigDecimal("100.00"));

        Sale sale = new Sale();
        ReflectionTestUtils.setField(sale, "id", saleId);
        sale.setTotalAmount(new BigDecimal("100.00"));
        sale.setDate(LocalDateTime.now(clock));
        sale.setItems(List.of(item));

        when(saleRepository.findByIdAndCompanyId(saleId, companyId))
                .thenReturn(Optional.of(sale));

        SaleResponseDTO response = service.findById(saleId, companyId);

        assertEquals(saleId, response.id());
        assertEquals(new BigDecimal("100.00"), response.totalAmount());
    }

    @Test
    void shouldThrowWhenSaleNotFound() {

        UUID saleId = UUID.randomUUID();

        when(saleRepository.findByIdAndCompanyId(saleId, companyId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.findById(saleId, companyId));
    }

    @Test
    void shouldReturnEmptyListWhenNoSales() {

        when(saleRepository.findAllByCompanyId(companyId))
                .thenReturn(List.of());

        List<SaleResponseDTO> result = service.findAll(companyId);

        assertTrue(result.isEmpty());
    }
}
