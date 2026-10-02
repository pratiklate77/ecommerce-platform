package com.ecommerce.inventory_service.repository;

import com.ecommerce.inventory_service.model.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findBySku(String sku);

    boolean existsBySku(String sku);

    List<InventoryItem> findByActiveTrueOrderBySkuAsc();

    /**
     * Loads an item with a pessimistic write lock so reserve/release operations
     * do not read a stale {@code availableQuantity}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.sku = :sku")
    Optional<InventoryItem> findForUpdateBySku(@Param("sku") String sku);
}
