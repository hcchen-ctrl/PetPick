package com.petpick.petpick.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "orders") // 注意：order 是保留字
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "status", nullable = false)
    private String status = "Pending";

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "addr", length = 100)
    private String addr;

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "receiver_phone")
    private String receiverPhone;

    @Column(name = "shipping_type")
    private String shippingType;

    @Column(name = "store_id")
    private String storeId;

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "store_address")
    private String storeAddress;

    @Column(name = "logistics_id")
    private String logisticsId;

    @Column(name = "tracking_no")
    private String trackingNo;

}
