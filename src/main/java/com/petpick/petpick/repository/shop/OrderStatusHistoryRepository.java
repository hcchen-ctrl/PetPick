package com.petpick.petpick.repository.shop;

import java.util.List;

import com.petpick.petpick.entity.shop.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;


public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
  List<OrderStatusHistory> findByOrder_OrderIdOrderByCreatedAtAsc(Integer orderId);
}
