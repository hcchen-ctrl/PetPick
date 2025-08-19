package com.petpick.petpick.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "shipments")
@Data
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer orderId;
    private String type;        // CVS_C2C / HOME_TCAT ...
    private String status;      // STORE_SELECT / BOOKED / IN_TRANSIT / ARRIVED / PICKED_UP ...
    private String logisticsSubType; // UNIMARTC2C / FAMIC2C / HILIFEC2C / OKMARTC2C
    private String isCollection;     // Y/N

    // 選店回來的門市資訊
    private String cvsStoreId;
    private String cvsStoreName;
    private String cvsAddress;
    private String cvsTelephone;

    // 託運單資訊
    private String allPayLogisticsId; // 綠界物流單號
    private String shipmentNo;        // 例如 7-11 托運單號

    @Lob
    private String raw;               // JSON or KV for debug
    private LocalDateTime createdAt = LocalDateTime.now();
}
