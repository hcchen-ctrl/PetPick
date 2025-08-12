package com.petpick.petpick.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petpick.petpick.entity.OrderDetail;

public interface OrderItemRepository extends JpaRepository<OrderDetail, Object> {
    
}
