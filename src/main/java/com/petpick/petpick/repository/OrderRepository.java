package com.petpick.petpick.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.petpick.petpick.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Integer userId);

    @Query("select o from Order o where o.tradeNo = :tradeNo")
    Optional<Order> findByTradeNo(@Param("tradeNo") String tradeNo);

    @Query("select o from Order o where o.merchantTradeNo = :mtn")
    Optional<Order> findByMerchantTradeNo(@Param("mtn") String mtn);

}
