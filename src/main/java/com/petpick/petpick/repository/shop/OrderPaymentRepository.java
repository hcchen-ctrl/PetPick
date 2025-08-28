package com.petpick.petpick.repository.shop;

import java.util.List;
import java.util.Optional;

import com.petpick.petpick.entity.shop.OrderPayment;
import org.springframework.data.jpa.repository.JpaRepository;


public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {
  List<OrderPayment> findByOrder_OrderId(Integer orderId);
  Optional<OrderPayment> findFirstByOrder_OrderIdAndStatusOrderByPaidAtDesc(Integer orderId, String status);
  Optional<OrderPayment> findByTradeNo(String tradeNo);
}
