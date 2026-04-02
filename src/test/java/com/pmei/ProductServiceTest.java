package com.pmei;

import com.pmei.company.model.Company;
import com.pmei.company.repository.CompanyRepository;
import com.pmei.shared.exception.ResourceNotFoundException;
import com.pmei.stock.dto.ProductRequestDTO;
import com.pmei.stock.dto.ProductResponseDTO;
import com.pmei.stock.model.Product;
import com.pmei.stock.repository.ProductRepository;
import com.pmei.stock.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CompanyRepository companyRepository;

    private ProductService service;

    private UUID companyId;
    private UUID productId;
    private Company company;

    @BeforeEach
    void setup() {
        service = new ProductService(productRepository, companyRepository);

        companyId = UUID.randomUUID();
        productId = UUID.randomUUID();

        company = new Company();
        ReflectionTestUtils.setField(company, "id", companyId);
    }

    @Test
    void shouldCreateProductSuccessfully() {

        ProductRequestDTO dto = new ProductRequestDTO("Notebook", new BigDecimal("2000"), 10, 3);

        Product saved = new Product();
        ReflectionTestUtils.setField(saved, "id", productId);
        saved.setName("Notebook");
        saved.setPrice(new BigDecimal("2000"));
        saved.setQuantity(10);
        saved.setMinimumStock(3);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponseDTO response = service.create(dto, companyId);

        assertEquals("Notebook", response.getName());
        assertEquals(10, response.getQuantity());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldThrowWhenCompanyNotFoundOnCreate() {

        ProductRequestDTO dto = new ProductRequestDTO("Notebook", new BigDecimal("2000"), 10, 3);

        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(dto, companyId));

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldFindProductByIdSuccessfully() {

        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", productId);
        product.setName("Mouse");
        product.setPrice(new BigDecimal("150.00"));
        product.setQuantity(5);
        product.setMinimumStock(2);

        when(productRepository.findByIdAndCompanyId(productId, companyId))
                .thenReturn(Optional.of(product));

        ProductResponseDTO response = service.findById(productId, companyId);

        assertEquals("Mouse", response.getName());
        assertEquals(5, response.getQuantity());
    }

    @Test
    void shouldThrowWhenProductNotFound() {

        when(productRepository.findByIdAndCompanyId(productId, companyId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.findById(productId, companyId));
    }

    @Test
    void shouldUpdateProductSuccessfully() {

        Product existing = new Product();
        ReflectionTestUtils.setField(existing, "id", productId);
        existing.setName("Old mouse");
        existing.setPrice(new BigDecimal("100.00"));
        existing.setQuantity(5);
        existing.setMinimumStock(2);

        ProductRequestDTO dto = new ProductRequestDTO("New mouse", new BigDecimal("120.00"), 8, 2);

        when(productRepository.findByIdAndCompanyId(productId, companyId))
                .thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenReturn(existing);

        ProductResponseDTO response = service.update(productId, dto, companyId);

        assertEquals("New mouse", response.getName());
        assertEquals(new BigDecimal("120.00"), response.getPrice());
        assertEquals(8, response.getQuantity());
    }

    @Test
    void shouldDeleteProductSuccessfully() {

        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", productId);

        when(productRepository.findByIdAndCompanyId(productId, companyId))
                .thenReturn(Optional.of(product));

        assertDoesNotThrow(() -> service.delete(productId, companyId));

        verify(productRepository).delete(product);
    }

    @Test
    void shouldReturnAllProductsByCompany() {

        Product p1 = new Product();
        ReflectionTestUtils.setField(p1, "id", UUID.randomUUID());
        p1.setName("Product 1");
        p1.setPrice(new BigDecimal("10"));
        p1.setQuantity(5);
        p1.setMinimumStock(1);

        when(productRepository.findAllByCompanyId(companyId)).thenReturn(List.of(p1));

        List<ProductResponseDTO> result = service.findAll(companyId);

        assertEquals(1, result.size());
        assertEquals("Product 1", result.get(0).getName());
    }
}
