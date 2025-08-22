package com.petpick.petpick.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity @Table(name="order_payments")
@Data
public class OrderPayment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) 
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY) 
  @JoinColumn(name="order_id", nullable=false)
  private Order order;
  private String gateway;
  private Integer amount;
  private String merchantTradeNo;
  private String tradeNo;
  private String status;     // INIT/SUCCESS/FAIL/REFUND
  private LocalDateTime paidAt;
  private String failReason;
  @Column(columnDefinition = "json") 
  private String payloadJson;
  @Column(name="created_at", insertable=false, updatable=false)
  private LocalDateTime createdAt;
}
