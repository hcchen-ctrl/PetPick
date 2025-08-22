package com.petpick.petpick.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petpick.petpick.entity.OrderStatusHistory;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
  List<OrderStatusHistory> findByOrder_OrderIdOrderByCreatedAtAsc(Integer orderId);
}
