package com.petpick.petpick.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;              // ← 確認用 JPA 的
import org.springframework.data.repository.query.Param;

import com.petpick.petpick.dto.OrderDetailDTO;
import com.petpick.petpick.entity.OrderDetail;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    List<OrderDetail> findByOrder_OrderId(Integer orderId);

    @Query("""
        select new com.petpick.petpick.dto.OrderDetailDTO(
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