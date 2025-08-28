package com.petpick.petpick.repository.shop;

import java.util.Optional;

import com.petpick.petpick.entity.shop.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findTopByOrderIdOrderByIdDesc(Integer orderId);
}
