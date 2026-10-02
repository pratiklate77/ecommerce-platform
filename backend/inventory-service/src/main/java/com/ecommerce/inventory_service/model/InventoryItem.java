package com.ecommerce.inventory_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Stock level for a single product (identified by its unique SKU).
 *
 * <p>{@code availableQuantity} is what can be promised to a customer today and
 * {@code reservedQuantity} is stock already held for pending orders. The two
 * together represent the on-hand stock. {@link jakarta.persistence.Version}
 * optimistic locking protects concurrent reserve/release updates.</p>
 */
@Entity
@Table(name = "inventory_items", uniqueConstraints = @UniqueConstraint(name = "uk_inventory_sku", columnNames = "sku"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Builder.Default
    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity = 0;

    @Builder.Default
    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Total on-hand stock (available + reserved). */
    public int totalOnHand() {
        return availableQuantity + reservedQuantity;
    }

    public boolean isInStock(int requested) {
        return availableQuantity >= requested;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
