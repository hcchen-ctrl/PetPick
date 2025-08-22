package com.petpick.petpick.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petpick.petpick.entity.OrderPayment;

public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {
  List<OrderPayment> findByOrder_OrderId(Integer orderId);
  Optional<OrderPayment> findFirstByOrder_OrderIdAndStatusOrderByPaidAtDesc(Integer orderId, String status);
  Optional<OrderPayment> findByTradeNo(String tradeNo);
}
