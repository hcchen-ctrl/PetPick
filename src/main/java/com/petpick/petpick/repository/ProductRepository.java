package com.petpick.petpick.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.petpick.petpick.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    // 可以加入自訂查詢方法，如：List<Product> findByIsActiveTrue();
}
