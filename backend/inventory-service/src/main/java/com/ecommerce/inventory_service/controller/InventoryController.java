package com.ecommerce.inventory_service.controller;

import com.ecommerce.inventory_service.dto.AvailabilityResponse;
import com.ecommerce.inventory_service.dto.InventoryItemRequest;
import com.ecommerce.inventory_service.dto.InventoryItemResponse;
import com.ecommerce.inventory_service.dto.ReserveRequest;
import com.ecommerce.inventory_service.dto.StockAdjustRequest;
import com.ecommerce.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /** Only admins may manage inventory. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/items")
    public ResponseEntity<InventoryItemResponse> create(@Valid @RequestBody InventoryItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.create(request));
    }

    /** Public - anyone can browse stock levels without logging in. */
    @GetMapping("/items")
    public List<InventoryItemResponse> list() {
        return inventoryService.list();
    }

    /** Public - anyone can view a stock item without logging in. */
    @GetMapping("/items/{id}")
    public InventoryItemResponse get(@PathVariable Long id) {
        return inventoryService.get(id);
    }

    /** Public - anyone can look up stock by SKU without logging in. */
    @GetMapping("/items/sku/{sku}")
    public InventoryItemResponse getBySku(@PathVariable String sku) {
        return inventoryService.getBySku(sku);
    }

    /** Only admins may manage inventory. */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/items/{id}")
    public InventoryItemResponse update(@PathVariable Long id, @Valid @RequestBody InventoryItemRequest request) {
        return inventoryService.update(id, request);
    }

    /** Only admins may manage inventory. */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        inventoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Public - anyone can check availability without logging in. */
    @GetMapping("/availability/{sku}")
    public AvailabilityResponse availability(@PathVariable String sku) {
        return inventoryService.availability(sku);
    }

    /** Only admins may manage inventory. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/items/adjust")
    public InventoryItemResponse adjustStock(@Valid @RequestBody StockAdjustRequest request) {
        return inventoryService.adjustStock(request);
    }

    /** Only admins may manage inventory. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reserve")
    public ResponseEntity<List<ReserveRequest.ReserveItem>> reserve(@Valid @RequestBody ReserveRequest request) {
        return ResponseEntity.ok(inventoryService.reserve(request));
    }

    /** Only admins may manage inventory. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/release/{orderId}")
    public ResponseEntity<Integer> release(@PathVariable Long orderId) {
        return ResponseEntity.ok(inventoryService.release(orderId));
    }
}
