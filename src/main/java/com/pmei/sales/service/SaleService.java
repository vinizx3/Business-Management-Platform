package com.pmei.sales.service;

import com.pmei.company.model.Company;
import com.pmei.company.repository.CompanyRepository;
import com.pmei.financial.dto.FinancialRequest;
import com.pmei.financial.model.RecurrenceType;
import com.pmei.financial.model.TransactionType;
import com.pmei.financial.service.FinancialService;
import com.pmei.sales.dto.SaleItemRequestDTO;
import com.pmei.sales.dto.SaleItemResponseDTO;
import com.pmei.sales.dto.SaleRequestDTO;
import com.pmei.sales.dto.SaleResponseDTO;
import com.pmei.sales.model.Sale;
import com.pmei.sales.model.SaleItem;
import com.pmei.sales.repository.SaleRepository;
import com.pmei.shared.exception.ResourceNotFoundException;
import com.pmei.stock.model.Product;
import com.pmei.stock.service.StockService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for handling sales operations.
 *
 * This service manages:
 * - Sale creation
 * - Retrieval of sales data
 * - Integration with stock and financial systems
 *
 * Ensures that:
 * - Stock is properly reduced
 * - Financial transactions are recorded
 * - Total sale amount is correctly calculated
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SaleService {

    private final SaleRepository saleRepository;
    private final CompanyRepository companyRepository;
    private final StockService stockService;
    private final FinancialService financialService;
    private final Clock clock;

    /**
     * Creates a new sale.
     *
     * Flow:
     * - Validates company existence
     * - Removes stock for each item
     * - Calculates total amount
     * - Persists sale and items
     * - Registers financial income
     *
     * @param request Sale request containing items
     * @param companyId Company identifier
     * @return SaleResponseDTO with sale details
     */
    public SaleResponseDTO createSale(SaleRequestDTO request, UUID companyId){

        // Ensure company exists
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found."));

        List<SaleItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // Process each item in the sale
        for (SaleItemRequestDTO itemDTO : request.getItems()) {

            // Remove stock and retrieve product
            Product product = stockService.removeStockAndReturn(
                    itemDTO.getProductId(), itemDTO.getQuantity(), companyId);

            // Calculate item subtotal
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity()));

            items.add(SaleItem.builder()
                    .product(product)
                    .quantity(itemDTO.getQuantity())
                    .price(product.getPrice())
                    .build());

            total = total.add(subtotal);
        }

        // Create sale entity
        Sale sale = Sale.builder()
                .date(LocalDateTime.now(clock))
                .totalAmount(total)
                .company(company)
                .items(items)
                .build();

        // Link items to sale (bidirectional relationship)
        items.forEach(item -> item.setSale(sale));

        Sale saved = saleRepository.save(sale);

        // Register financial income from the sale
        financialService.register(
                new FinancialRequest(
                        "Sale #" + saved.getId(),
                        total,
                        TransactionType.INCOME,
                        RecurrenceType.VARIABLE,
                        LocalDateTime.now(clock)
                ),
                companyId
        );

        log.info("Sale created | company={} | sale={} | total={}", companyId, saved.getId(), total);

        return toDTO(saved);
    }

    /**
     * Retrieves a sale by ID, ensuring it belongs to the company.
     */
    public SaleResponseDTO findById(UUID id, UUID companyId) {
        Sale sale = saleRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + id));
        return toDTO(sale);
    }

    /**
     * Retrieves all sales for a company.
     */
    public List<SaleResponseDTO> findAll(UUID companyId) {
        return saleRepository.findAllByCompanyId(companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Converts Sale entity to DTO.
     *
     * Includes item details such as:
     * - Product ID
     * - Product name
     * - Quantity
     * - Price at the time of sale
     */
    private SaleResponseDTO toDTO(Sale sale) {
        return new SaleResponseDTO(
                sale.getId(),
                sale.getTotalAmount(),
                sale.getDate(),
                sale.getItems().stream()
                        .map(item -> new SaleItemResponseDTO(
                                item.getProduct().getId(),
                                item.getProduct().getName(),
                                item.getQuantity(),
                                item.getPrice()
                        ))
                        .toList()
        );
    }
}