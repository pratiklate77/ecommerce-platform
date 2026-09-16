package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.exception.ConflictException;
import com.ecommerce.product_service.exception.ResourceNotFoundException;
import com.ecommerce.product_service.model.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    private ProductRequest request() {
        return new ProductRequest(
                "sku-123",
                "Wireless Mouse",
                "A comfortable wireless mouse",
                "Electronics",
                new BigDecimal("29.99"),
                "USD",
                "https://example.com/mouse.png",
                true
        );
    }

    private Product product(Long id) {
        return Product.builder()
                .id(id)
                .sku("SKU-123")
                .name("Wireless Mouse")
                .category("Electronics")
                .price(new BigDecimal("29.99"))
                .currency("USD")
                .active(true)
                .build();
    }

    @Test
    void createNormalizesSkuAndSaves() {
        when(productRepository.existsBySku("SKU-123")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        var response = productService.create(request());

        assertEquals("SKU-123", response.sku());
        assertEquals("USD", response.currency());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createRejectsDuplicateSku() {
        when(productRepository.existsBySku("SKU-123")).thenReturn(true);

        assertThrows(ConflictException.class, () -> productService.create(request()));
    }

    @Test
    void getReturnsProductWhenFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L)));

        var response = productService.get(1L);

        assertEquals(1L, response.id());
        assertEquals("SKU-123", response.sku());
    }

    @Test
    void getThrowsWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.get(99L));
    }

    @Test
    void searchListsAllActiveWhenQueryIsBlank() {
        when(productRepository.findByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(product(1L)));

        var results = productService.search("  ");

        assertEquals(1, results.size());
        verify(productRepository).findByActiveTrueOrderByCreatedAtDesc();
    }

    @Test
    void searchDelegatesToRepositoryWhenQueryPresent() {
        when(productRepository.searchActive("mouse")).thenReturn(List.of(product(1L)));

        var results = productService.search(" mouse ");

        assertEquals(1, results.size());
        verify(productRepository).searchActive("mouse");
    }

    @Test
    void updateChangesFieldsAndKeepsId() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L)));
        when(productRepository.findBySku("SKU-123")).thenReturn(Optional.of(product(1L)));

        var response = productService.update(1L, request());

        assertEquals(1L, response.id());
        assertEquals("Wireless Mouse", response.name());
        verify(productRepository).findBySku("SKU-123");
    }

    @Test
    void updateRejectsSkuOwnedByAnotherProduct() {
        Product existing = product(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.findBySku("SKU-123")).thenReturn(Optional.of(product(2L)));

        assertThrows(ConflictException.class, () -> productService.update(1L, request()));
    }

    @Test
    void deleteRemovesExistingProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L)));

        productService.delete(1L);

        verify(productRepository).delete(any(Product.class));
    }

    @Test
    void deleteThrowsWhenProductMissing() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.delete(1L));
    }
}