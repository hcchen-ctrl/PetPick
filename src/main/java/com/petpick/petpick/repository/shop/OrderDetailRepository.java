package com.petpick.petpick.repository.shop;

import java.util.List;

import com.petpick.petpick.DTO.shop.OrderDetailDTO;
import com.petpick.petpick.entity.shop.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;              // ← 確認用 JPA 的
import org.springframework.data.repository.query.Param;



public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    List<OrderDetail> findByOrder_OrderId(Integer orderId);

    @Query("""
        select new com.petpick.petpick.DTO.shop.OrderDetailDTO(
            d.id,                 
            d.order.orderId,      
            d.product.productId,
            d.product.pname,
            d.unitPrice,
            d.quantity,
            d.subtotal
        )
        from OrderDetail d
        where d.order.orderId = :orderId
    """)
    List<OrderDetailDTO> findDTOsByOrderId(@Param("orderId") Integer orderId);
}