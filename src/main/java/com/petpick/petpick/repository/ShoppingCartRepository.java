package com.petpick.petpick.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.petpick.petpick.dto.CartProductView;
import com.petpick.petpick.entity.Product;
import com.petpick.petpick.entity.ShoppingCartItem;

import jakarta.transaction.Transactional;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCartItem, Integer> {

    @Query("SELECT sc FROM ShoppingCartItem sc WHERE sc.userId = :userId AND sc.product.productId = :productId")
    Optional<ShoppingCartItem> findByUserIdAndProductId(@Param("userId") Integer userId, @Param("productId") Integer productId);

    // 查詢指定使用者的所有購物車項目
    List<ShoppingCartItem> findByUserId(Integer userId);

    // 查詢指定使用者 + 指定商品
    Optional<ShoppingCartItem> findByUserIdAndProduct(Integer userId, Product product);

    @Query("""
    SELECT
        sc.cartId   AS cartId,
        sc.quantity AS quantity,
        sc.userId   AS userId,
        sc.addedAt  AS addedAt,
        p.productId AS productId,
        p.pname     AS pname,
        p.imageUrl  AS imageUrl,
        p.price     AS price,
        p.stock     AS stock
    FROM ShoppingCartItem sc
    JOIN sc.product p
    WHERE sc.userId = :userId
""")
    List<CartProductView> findCartItemsWithProductByUserId(@Param("userId") Integer userId);

    @Modifying
    @Transactional
    int deleteByUserId(Integer userId);
}
