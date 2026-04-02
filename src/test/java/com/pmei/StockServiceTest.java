package com.pmei;

import com.pmei.shared.exception.BusinessException;
import com.pmei.shared.exception.ResourceNotFoundException;
import com.pmei.stock.dto.ProductResponseDTO;
import com.pmei.stock.dto.StockMovementResponseDTO;
import com.pmei.stock.model.MovementType;
import com.pmei.stock.model.Product;
import com.pmei.stock.model.StockMovement;
import com.pmei.stock.repository.ProductRepository;
import com.pmei.stock.repository.StockMovementRepository;
import com.pmei.stock.service.ProductService;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProductRepository productRepository;

    private Clock clock;
    private StockService service;

    private UUID companyId;
    private UUID productId;
    private Product product;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(
                LocalDate.of(2026, 1, 1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );

        service = new StockService(productService, stockMovementRepository, productRepository, clock);

        companyId = UUID.randomUUID();
        productId = UUID.randomUUID();

        product = new Product();
        ReflectionTestUtils.setField(product, "id", productId);
        product.setName("Notebook");
        product.setPrice(new BigDecimal("2000.00"));
        product.setQuantity(10);
        product.setMinimumStock(3);
    }

    @Test
    void shouldAddStockSuccessfully() {

        when(productService.findProductOrThrow(productId, companyId)).thenReturn(product);

        service.addStock(productId, 5, companyId);

        assertEquals(15, product.getQuantity());
        verify(stockMovementRepository).save(argThat(m ->
                m.getType() == MovementType.ENTRY &&
                        m.getQuantity() == 5 &&
                        m.getProduct().equals(product)
        ));
    }

    @Test
    void shouldThrowWhenProductNotFoundOnAddStock() {

        when(productService.findProductOrThrow(productId, companyId))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        assertThrows(ResourceNotFoundException.class,
                () -> service.addStock(productId, 5, companyId));

        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void shouldRemoveStockSuccessfully() {

        when(productService.findProductOrThrow(productId, companyId)).thenReturn(product);

        service.removeStock(productId, 4, companyId);

        assertEquals(6, product.getQuantity());
        verify(stockMovementRepository).save(argThat(m ->
                m.getType() == MovementType.EXIT &&
                        m.getQuantity() == 4
        ));
    }

    @Test
    void shouldThrowWhenInsufficientStock() {

        when(productService.findProductOrThrow(productId, companyId)).thenReturn(product);

        assertThrows(BusinessException.class,
                () -> service.removeStock(productId, 20, companyId));

        assertEquals(10, product.getQuantity());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void shouldTriggerLowStockAlertAfterRemoval() {

        product.setQuantity(4);
        product.setMinimumStock(5);

        when(productService.findProductOrThrow(productId, companyId)).thenReturn(product);

        assertDoesNotThrow(() -> service.removeStock(productId, 2, companyId));

        assertEquals(2, product.getQuantity());
        verify(stockMovementRepository).save(any());
    }

    @Test
    void shouldNotTriggerAlertWhenStockIsAboveMinimum() {

        product.setQuantity(10);
        product.setMinimumStock(3);

        when(productService.findProductOrThrow(productId, companyId)).thenReturn(product);

        assertDoesNotThrow(() -> service.removeStock(productId, 2, companyId));

        assertEquals(8, product.getQuantity());
    }

    @Test
    void shouldRemoveStockAndReturnUpdatedProduct() {

        Product updatedProduct = new Product();
        ReflectionTestUtils.setField(updatedProduct, "id", productId);
        updatedProduct.setName("Notebook");
        updatedProduct.setPrice(new BigDecimal("2000.00"));
        updatedProduct.setQuantity(6);
        updatedProduct.setMinimumStock(3);

        when(productService.findProductOrThrow(productId, companyId))
                .thenReturn(product)
                .thenReturn(updatedProduct);

        Product result = service.removeStockAndReturn(productId, 4, companyId);

        assertEquals(6, result.getQuantity());
        verify(stockMovementRepository).save(any());
    }

    @Test
    void shouldReturnLowStockProducts() {

        Product lowProduct = new Product();
        ReflectionTestUtils.setField(lowProduct, "id", UUID.randomUUID());
        lowProduct.setName("Pen");
        lowProduct.setPrice(new BigDecimal("2.00"));
        lowProduct.setQuantity(1);
        lowProduct.setMinimumStock(10);

        when(productRepository.findLowStockProducts(companyId))
                .thenReturn(List.of(lowProduct));

        List<ProductResponseDTO> result = service.getLowStockProducts(companyId);

        assertEquals(1, result.size());
        assertEquals("Pen", result.get(0).getName());
        assertEquals(1, result.get(0).getQuantity());
    }

    @Test
    void shouldReturnEmptyListWhenNoLowStockProducts() {

        when(productRepository.findLowStockProducts(companyId))
                .thenReturn(List.of());

        List<ProductResponseDTO> result = service.getLowStockProducts(companyId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnProductMovements() {

        StockMovement movement = StockMovement.builder()
                .product(product)
                .quantity(5)
                .type(MovementType.ENTRY)
                .date(LocalDateTime.now(clock))
                .build();
        ReflectionTestUtils.setField(movement, "id", UUID.randomUUID());

        when(productService.findProductOrThrow(productId, companyId)).thenReturn(product);
        when(stockMovementRepository.findByProductId(productId)).thenReturn(List.of(movement));

        List<StockMovementResponseDTO> result = service.getProductMovements(productId, companyId);

        assertEquals(1, result.size());
        assertEquals(MovementType.ENTRY, result.get(0).type());
        assertEquals(5, result.get(0).quantity());
    }
}

