package com.pmei.stock.service;

import com.pmei.company.model.Company;
import com.pmei.company.repository.CompanyRepository;
import com.pmei.shared.exception.ResourceNotFoundException;
import com.pmei.stock.dto.ProductRequestDTO;
import com.pmei.stock.dto.ProductResponseDTO;
import com.pmei.stock.model.Product;
import com.pmei.stock.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for managing products.
 *
 * Handles:
 * - Product creation, update, deletion
 * - Product retrieval
 *
 * Ensures that all operations are scoped to a specific company.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;

    /**
     * Creates a new product for a company.
     *
     * Rule:
     * - Company must exist
     */
    public ProductResponseDTO create(ProductRequestDTO dto, UUID companyId) {

        // Ensure company exists
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found." + companyId));

        Product product = Product.builder()
                .name(dto.getName())
                .price(dto.getPrice())
                .quantity(dto.getQuantity())
                .minimumStock(dto.getMinimumStock())
                .company(company)
                .build();

        Product saved = productRepository.save(product);

        log.info("Product created | company={} | product={}", companyId, saved.getId());

        return toDTO(saved);
    }

    /**
     * Returns all products for a company.
     */
    public List<ProductResponseDTO> findAll(UUID companyId) {
        return productRepository.findAllByCompanyId(companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Returns a product by ID.
     */
    public ProductResponseDTO findById(UUID id, UUID companyId) {
        Product product = findProductOrThrow(id, companyId);
        return toDTO(product);
    }

    /**
     * Updates product data.
     */
    public ProductResponseDTO update(UUID id, ProductRequestDTO dto, UUID companyId) {

        Product product = findProductOrThrow(id, companyId);

        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setQuantity(dto.getQuantity());
        product.setMinimumStock(dto.getMinimumStock());

        log.info("Product updated | company={} | product={}", companyId, id);

        return toDTO(productRepository.save(product));
    }

    /**
     * Deletes a product.
     */
    public void delete(UUID id, UUID companyId){
        Product product = findProductOrThrow(id, companyId);
        productRepository.delete(product);
        log.info("Product deleted | company={} | product={}", companyId, id);
    }

    /**
     * Retrieves a product or throws exception if not found
     * or not associated with the company.
     */
    public Product findProductOrThrow(UUID productId, UUID companyId){
        return productRepository.findByIdAndCompanyId(productId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + companyId));
    }

    /**
     * Converts Product entity to DTO.
     */
    private ProductResponseDTO toDTO(Product product){

        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getQuantity(),
                product.getMinimumStock()
        );
    }
}
