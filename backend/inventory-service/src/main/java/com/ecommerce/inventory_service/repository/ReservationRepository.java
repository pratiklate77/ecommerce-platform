package com.ecommerce.inventory_service.repository;

import com.ecommerce.inventory_service.model.Reservation;
import com.ecommerce.inventory_service.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReservationId(String reservationId);

    List<Reservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);

    boolean existsByOrderIdAndSku(Long orderId, String sku);
}
