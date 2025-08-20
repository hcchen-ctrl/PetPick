
package com.petpick.petpick.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petpick.petpick.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
