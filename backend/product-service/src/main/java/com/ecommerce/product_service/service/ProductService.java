package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.exception.ConflictException;
import com.ecommerce.product_service.exception.ResourceNotFoundException;
import com.ecommerce.product_service.model.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = normalizeSku(request.sku());

        if (productRepository.existsBySku(sku)) {
            throw new ConflictException("A product with SKU " + sku + " already exists");
        }

        Product product = Product.builder()
                .sku(sku)
                .name(request.name())
                .description(request.description())
                .category(request.category())
                .price(request.price())
                .currency(request.currency().toUpperCase())
                .imageUrl(request.imageUrl())
                .active(request.active())
                .build();

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(findByIdOrThrow(id));
    }

    /**
     * Lists active products, optionally filtered by a keyword on name/category.
     * A blank or missing query returns all active products.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> search(String query) {
        List<Product> products = (query == null || query.isBlank())
                ? productRepository.findByActiveTrueOrderByCreatedAtDesc()
                : productRepository.searchActive(query.trim());
        return products.stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findByIdOrThrow(id);
        String sku = normalizeSku(request.sku());

        Optional<Product> existing = productRepository.findBySku(sku);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new ConflictException("A product with SKU " + sku + " already exists");
        }

        product.setSku(sku);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategory(request.category());
        product.setPrice(request.price());
        product.setCurrency(request.currency().toUpperCase());
        product.setImageUrl(request.imageUrl());
        product.setActive(request.active());

        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(findByIdOrThrow(id));
    }

    public Product findByIdOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase();
    }
}

