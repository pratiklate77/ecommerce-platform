package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.AvailabilityResponse;
import com.ecommerce.inventory_service.dto.InventoryItemRequest;
import com.ecommerce.inventory_service.dto.InventoryItemResponse;
import com.ecommerce.inventory_service.dto.ReserveRequest;
import com.ecommerce.inventory_service.dto.StockAdjustRequest;
import com.ecommerce.inventory_service.exception.ConflictException;
import com.ecommerce.inventory_service.exception.ResourceNotFoundException;
import com.ecommerce.inventory_service.model.InventoryItem;
import com.ecommerce.inventory_service.model.Reservation;
import com.ecommerce.inventory_service.model.ReservationStatus;
import com.ecommerce.inventory_service.repository.InventoryItemRepository;
import com.ecommerce.inventory_service.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages product stock: CRUD, adjustments, availability reads and the
 * reserve / release lifecycle. Reads are served directly from the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository itemRepository;
    private final ReservationRepository reservationRepository;

    // ------------------------------------------------------------------ CRUD

    @Transactional
    public InventoryItemResponse create(InventoryItemRequest request) {
        String sku = normalizeSku(request.sku());
        if (itemRepository.existsBySku(sku)) {
            throw new ConflictException("An inventory item with SKU " + sku + " already exists");
        }
        InventoryItem item = InventoryItem.builder()
                .sku(sku)
                .productId(request.productId())
                .productName(request.productName())
                .availableQuantity(request.availableQuantity())
                .reorderLevel(request.reorderLevel())
                .build();
        return InventoryItemResponse.from(itemRepository.save(item));
    }

    @Transactional
    public InventoryItemResponse update(Long id, InventoryItemRequest request) {
        InventoryItem item = findByIdOrThrow(id);
        String sku = normalizeSku(request.sku());

        Optional<InventoryItem> existing = itemRepository.findBySku(sku);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new ConflictException("An inventory item with SKU " + sku + " already exists");
        }

        item.setSku(sku);
        item.setProductId(request.productId());
        item.setProductName(request.productName());
        // availableQuantity is managed via adjust/reserve/release, not here.
        item.setReorderLevel(request.reorderLevel());
        return InventoryItemResponse.from(item);
    }

    @Transactional
    public void delete(Long id) {
        InventoryItem item = findByIdOrThrow(id);
        if (item.getReservedQuantity() > 0) {
            throw new ConflictException("Cannot deactivate " + item.getSku()
                    + " while it still holds reserved stock");
        }
        item.setActive(false);
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse get(Long id) {
        return InventoryItemResponse.from(findByIdOrThrow(id));
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse getBySku(String sku) {
        return InventoryItemResponse.from(findBySkuOrThrow(normalizeSku(sku)));
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> list() {
        return itemRepository.findByActiveTrueOrderBySkuAsc().stream()
                .map(InventoryItemResponse::from)
                .toList();
    }

    // --------------------------------------------------------------- reads

    /**
     * Availability read straight from the database.
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse availability(String sku) {
        sku = normalizeSku(sku);
        InventoryItem item = findBySkuOrThrow(sku);
        return new AvailabilityResponse(
                sku, item.getAvailableQuantity(), item.getReservedQuantity(), item.isInStock(1));
    }

    // ------------------------------------------------------------- mutations

    @Transactional
    public InventoryItemResponse adjustStock(StockAdjustRequest request) {
        String sku = normalizeSku(request.sku());
        InventoryItem item = findBySkuForUpdateOrThrow(sku);
        int newAvailable = item.getAvailableQuantity() + request.delta();
        if (newAvailable < 0) {
            throw new ConflictException("Insufficient stock for " + sku
                    + ": attempted delta " + request.delta() + " on " + item.getAvailableQuantity());
        }
        item.setAvailableQuantity(newAvailable);
        return InventoryItemResponse.from(item);
    }

    /**
     * Manual reserve for a pending order (used by the admin REST path).
     * Skips SKUs the order created but inventory has not seeded yet.
     */
    @Transactional
    public List<ReserveRequest.ReserveItem> reserve(ReserveRequest request) {
        request.items().forEach(item -> reserveSingle(request.orderId(), item.sku(), item.quantity()));
        return request.items();
    }

    private void reserveSingle(Long orderId, String sku, int quantity) {
        if (quantity <= 0) {
            return;
        }
        Optional<InventoryItem> maybe = itemRepository.findForUpdateBySku(normalizeSku(sku));
        if (maybe.isEmpty()) {
            log.warn("No inventory item for SKU {} (order {}); skipping reservation", sku, orderId);
            return;
        }
        InventoryItem item = maybe.get();
        if (!item.isInStock(quantity)) {
            log.warn("Insufficient stock for SKU {} in order {}: need {} have {}",
                    sku, orderId, quantity, item.getAvailableQuantity());
            return;
        }

        Reservation reservation = Reservation.builder()
                .reservationId(UUID.randomUUID().toString())
                .orderId(orderId)
                .sku(item.getSku())
                .productId(item.getProductId())
                .quantity(quantity)
                .status(ReservationStatus.ACTIVE)
                .build();
        item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
        item.setReservedQuantity(item.getReservedQuantity() + quantity);

        reservationRepository.save(reservation);
        log.info("Reserved {}x{} for order {}", quantity, item.getSku(), orderId);
    }

    /**
     * Releases all active reservations for an order back to available stock
     * (used on cancellation). Returns the number of releases performed.
     */
    @Transactional
    public int release(Long orderId) {
        List<Reservation> active = reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);
        if (active.isEmpty()) {
            log.debug("No active reservations to release for order {}", orderId);
            return 0;
        }
        active.forEach(reservation -> {
            Optional<InventoryItem> maybe = itemRepository.findForUpdateBySku(reservation.getSku());
            if (maybe.isEmpty()) {
                log.warn("Inventory item {} missing while releasing order {}, marking reservation released anyway",
                        reservation.getSku(), orderId);
                reservation.setStatus(ReservationStatus.RELEASED);
                return;
            }
            InventoryItem item = maybe.get();
            int released = Math.min(reservation.getQuantity(), item.getReservedQuantity());
            item.setReservedQuantity(item.getReservedQuantity() - released);
            item.setAvailableQuantity(item.getAvailableQuantity() + released);
            reservation.setStatus(ReservationStatus.RELEASED);
        });
        log.info("Released {} reservation(s) for order {}", active.size(), orderId);
        return active.size();
    }

    // -------------------------------------------------------------- helpers

    private InventoryItem findByIdOrThrow(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + id));
    }

    private InventoryItem findBySkuOrThrow(String sku) {
        return itemRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + sku));
    }

    private InventoryItem findBySkuForUpdateOrThrow(String sku) {
        return itemRepository.findForUpdateBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + sku));
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase();
    }
}
