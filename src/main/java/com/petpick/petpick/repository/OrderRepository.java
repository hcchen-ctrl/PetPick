package com.petpick.petpick.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.petpick.petpick.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Integer>, JpaSpecificationExecutor<Order>{

    List<Order> findByUserIdOrderByCreatedAtDesc(Integer userId);

    Optional<Order> findByTradeNo(String tradeNo);

    Optional<Order> findByMerchantTradeNo(String merchantTradeNo);
}