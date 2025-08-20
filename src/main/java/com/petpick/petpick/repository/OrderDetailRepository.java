package com.petpick.petpick.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petpick.petpick.entity.OrderDetail;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    List<OrderDetail> findByOrder_OrderId(Integer orderId);
}