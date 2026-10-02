package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.InventoryItemRequest;
import com.ecommerce.inventory_service.dto.ReserveRequest;
import com.ecommerce.inventory_service.dto.StockAdjustRequest;
import com.ecommerce.inventory_service.exception.ConflictException;
import com.ecommerce.inventory_service.model.InventoryItem;
import com.ecommerce.inventory_service.model.Reservation;
import com.ecommerce.inventory_service.model.ReservationStatus;
import com.ecommerce.inventory_service.repository.InventoryItemRepository;
import com.ecommerce.inventory_service.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @InjectMocks
    private InventoryService inventoryService;

    @Mock
    private InventoryItemRepository itemRepository;

    @Mock
    private ReservationRepository reservationRepository;

    private InventoryItem item(long id, String sku, int available, int reserved) {
        return InventoryItem.builder()
                .id(id).sku(sku).productId(1L).productName("Mouse")
                .availableQuantity(available).reservedQuantity(reserved).reorderLevel(5)
                .active(true).build();
    }

    private InventoryItemRequest request() {
        return new InventoryItemRequest(" sku-1 ", 1L, "Mouse", 10, 5);
    }

    @Test
    void createNormalizesSkuAndSaves() {
        when(itemRepository.existsBySku("SKU-1")).thenReturn(false);
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = inventoryService.create(request());
        assertEquals("SKU-1", response.sku());
        assertEquals(10, response.availableQuantity());
        verify(itemRepository).save(any(InventoryItem.class));
    }

    @Test
    void createRejectsDuplicateSku() {
        when(itemRepository.existsBySku("SKU-1")).thenReturn(true);
        assertThrows(ConflictException.class, () -> inventoryService.create(request()));
    }

    @Test
    void reserveMovesStockFromAvailableToReserved() {
        InventoryItem prod = item(1L, "SKU-1", 10, 0);
        when(itemRepository.findForUpdateBySku("SKU-1")).thenReturn(Optional.of(prod));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.reserve(new ReserveRequest(10L,
                List.of(new ReserveRequest.ReserveItem("SKU-1", 3))));

        assertEquals(7, prod.getAvailableQuantity());
        assertEquals(3, prod.getReservedQuantity());
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertEquals(ReservationStatus.ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void reserveSkipsMissingSkuWithoutThrowing() {
        when(itemRepository.findForUpdateBySku("SKU-MISSING")).thenReturn(Optional.empty());

        inventoryService.reserve(new ReserveRequest(10L,
                List.of(new ReserveRequest.ReserveItem("SKU-MISSING", 3))));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void releaseRestoresStockToAvailable() {
        InventoryItem prod = item(1L, "SKU-1", 7, 3);
        Reservation reservation = Reservation.builder()
                .id(1L).reservationId("r1").orderId(10L).sku("SKU-1").productId(1L)
                .quantity(3).status(ReservationStatus.ACTIVE).build();
        when(reservationRepository.findByOrderIdAndStatus(10L, ReservationStatus.ACTIVE))
                .thenReturn(List.of(reservation));
        when(itemRepository.findForUpdateBySku("SKU-1")).thenReturn(Optional.of(prod));

        int released = inventoryService.release(10L);

        assertEquals(1, released);
        assertEquals(10, prod.getAvailableQuantity());
        assertEquals(0, prod.getReservedQuantity());
        assertEquals(ReservationStatus.RELEASED, reservation.getStatus());
    }

    @Test
    void adjustStockRejectsNegativeResult() {
        InventoryItem prod = item(1L, "SKU-1", 2, 0);
        when(itemRepository.findForUpdateBySku("SKU-1")).thenReturn(Optional.of(prod));

        assertThrows(ConflictException.class,
                () -> inventoryService.adjustStock(new StockAdjustRequest("SKU-1", -5)));
    }
}
