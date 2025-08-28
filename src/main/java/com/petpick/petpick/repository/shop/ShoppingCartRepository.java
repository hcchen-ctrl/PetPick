package com.petpick.petpick.repository.shop;

import java.util.List;
import java.util.Optional;

import com.petpick.petpick.DTO.shop.CartProductView;
import com.petpick.petpick.entity.shop.Product;
import com.petpick.petpick.entity.shop.ShoppingCartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;        // ← 改用 JPA 的
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional; // ← 改用 Spring 這個



public interface ShoppingCartRepository extends JpaRepository<ShoppingCartItem, Integer> {

    @Query("""
        select sc from ShoppingCartItem sc
        where sc.userId = :userId and sc.product.productId = :productId
    """)
    Optional<ShoppingCartItem> findByUserIdAndProductId(@Param("userId") Integer userId,
                                                        @Param("productId") Integer productId);

    List<ShoppingCartItem> findByUserId(Integer userId);

    Optional<ShoppingCartItem> findByUserIdAndProduct(Integer userId, Product product);

    @Query("""
        select
            sc.cartId   as cartId,
            sc.quantity as quantity,
            sc.userId   as userId,
            sc.addedAt  as addedAt,
            p.productId as productId,
            p.pname     as pname,
            p.imageUrl  as imageUrl,
            p.price     as price,
            p.stock     as stock
        from ShoppingCartItem sc
        join sc.product p
        where sc.userId = :userId
    """)
    List<CartProductView> findCartItemsWithProductByUserId(@Param("userId") Integer userId);

    // 衍生刪除查詢（不需 @Query），@Modifying + @Transactional 保險起見
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    int deleteByUserId(Integer userId);
}