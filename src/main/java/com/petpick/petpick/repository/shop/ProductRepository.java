package com.petpick.petpick.repository.shop;

import java.util.List;

import com.petpick.petpick.entity.shop.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    @Modifying
    @Query("""
        UPDATE Product p
           SET p.stock = p.stock - :qty
         WHERE p.productId = :pid
           AND p.stock >= :qty
    """)
    int decreaseStock(@Param("pid") Integer productId, @Param("qty") int qty);

    @Modifying
    @Query("""
        UPDATE Product p
           SET p.stock = p.stock + :qty
         WHERE p.productId = :pid
    """)
    int increaseStock(@Param("pid") Integer productId, @Param("qty") int qty);

    List<Product> findAllByPublishedTrue();
    List<Product> findAllByPublishedFalse();
}
