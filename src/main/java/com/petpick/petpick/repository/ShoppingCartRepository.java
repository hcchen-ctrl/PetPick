package com.petpick.petpick.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.petpick.petpick.dto.CartProductDTO;
import com.petpick.petpick.entity.Product;
import com.petpick.petpick.entity.ShoppingCartItem;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCartItem, Integer> {

    @Query("SELECT sc FROM ShoppingCartItem sc WHERE sc.userId = :userId AND sc.product.productId = :productId")
    Optional<ShoppingCartItem> findByUserIdAndProductId(@Param("userId") Integer userId, @Param("productId") Integer productId);

    // 查詢指定使用者的所有購物車項目
    List<ShoppingCartItem> findByUserId(Integer userId);

    // 查詢指定使用者 + 指定商品
    Optional<ShoppingCartItem> findByUserIdAndProduct(Integer userId, Product product);

    @Query("""
    SELECT new com.petpick.petpick.dto.CartProductDTO(
        sc.cartId, sc.userId, sc.quantity, sc.addedAt,
        p.productId, p.pname, p.imageUrl, p.price
    )
    FROM ShoppingCartItem sc
    JOIN sc.product p
    WHERE sc.userId = :userId
""")
    List<CartProductDTO> findCartItemsWithProductByUserId(@Param("userId") Integer userId);

}
