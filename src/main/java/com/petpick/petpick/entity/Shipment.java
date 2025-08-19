package com.petpick.petpick.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shipments")
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class Shipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer orderId;             // 關聯你的訂單
    private String type;                 // CVS / HOME
    private String subType;              // UNIMARTC2C ...
    private String isCollection;         // Y / N

    private String storeId;              // CVSStoreID
    private String storeName;
    private String storeAddress;
    private String storeTel;

    private String status;               // NEW / MAP_SELECTED / CREATED / SHIPPED / ARRIVED / PICKED ...
    private String merchantTradeNo;      // 你送綠界的唯一碼
    private String allPayLogisticsId;    // 綠界回傳物流單號
    private String rrm;                  // 其他欄位

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
