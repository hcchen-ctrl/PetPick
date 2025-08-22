package com.petpick.petpick.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petpick.petpick.entity.OrderShipment;


public interface OrderShipmentRepository extends JpaRepository<OrderShipment, Long> {
  List<OrderShipment> findByOrder_OrderIdOrderByCreatedAtAsc(Integer orderId);
  Optional<OrderShipment> findFirstByOrder_OrderIdOrderByCreatedAtAsc(Integer orderId); // 主出貨
  Optional<OrderShipment> findByTrackingNo(String trackingNo);
}

