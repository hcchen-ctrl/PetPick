package com.petpick.petpick.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petpick.petpick.entity.Shipment;

public interface ShipmentRepository extends JpaRepository<Shipment, Integer> {
    Optional<Shipment> findFirstByOrderIdOrderByIdDesc(Integer orderId);
}
