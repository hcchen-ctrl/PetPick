package com.petpick.petpick.repository.shop;

import java.util.List;
import java.util.Optional;

import com.petpick.petpick.entity.shop.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface OrderRepository extends JpaRepository<Order, Integer>, JpaSpecificationExecutor<Order>{

    List<Order> findByUserIdOrderByCreatedAtDesc(Integer userId);

    Optional<Order> findByTradeNo(String tradeNo);

    Optional<Order> findByMerchantTradeNo(String merchantTradeNo);

    Optional<Order> findByLogisticsId(String logisticsId);
}