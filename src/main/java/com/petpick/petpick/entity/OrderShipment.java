package com.petpick.petpick.entity;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.QueryRewriter.IdentityQueryRewriter;

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

@Entity
@Table(name="order_shipments")
@Data
public class OrderShipment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) 
  private Long id;
  @ManyToOne(fetch=FetchType.LAZY) 
  @JoinColumn(name="order_id", nullable=false)
  private Order order;
  private String shippingType;
  private String logisticsSubtype;
  private Boolean isCollection;
  private String receiverName, receiverPhone, receiverZip, receiverAddr;
  private String storeId, storeName, storeAddress, storeBrand;
  private String logisticsId, trackingNo;
  private String status;
  private LocalDateTime shippedAt, receivedAt, deliveredAt;

  @Column(columnDefinition = "json") 
  private String payloadJson;

  @Column(name="created_at", insertable=false, updatable=false)
  private LocalDateTime createdAt;
}
