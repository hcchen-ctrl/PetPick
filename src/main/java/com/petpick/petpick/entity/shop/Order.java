package com.petpick.petpick.entity.shop;

import java.time.LocalDateTime;

import com.petpick.petpick.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "orders") // 注意：order 是保留字
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "userid", nullable = false)
    private UserEntity user;


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

    @Column(name = "trade_no", unique = true, length = 50)
    private String tradeNo;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "merchant_trade_no", length = 20)
    private String merchantTradeNo;

    @Column(name = "store_brand")
    private String storeBrand;
    
    @Column(name = "receiver_zip")
    private String receiverZip;
    
    @Column(name = "logistics_status")
    private String logisticsStatus;
    
    @Column(name = "payment_gateway")
    private String paymentGateway;
    
    @Column(name = "payment_fail_reason")
    private String paymentFailReason;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    public String getMerchantTradeNo() {
        return merchantTradeNo;
    }

    public void setMerchantTradeNo(String merchantTradeNo) {
        this.merchantTradeNo = merchantTradeNo;
    }

}
