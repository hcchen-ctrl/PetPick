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

@Entity
@Table(name="order_status_history")
@Data
public class OrderStatusHistory {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY) 
  private Long id;

  @ManyToOne(fetch=FetchType.LAZY) 
  @JoinColumn(name="order_id", nullable=false)
  private Order order;
  private String fromStatus;
  private String toStatus;
  private String note;
  private String actor;
  
  @Column(name="created_at", insertable=false, updatable=false)
  private LocalDateTime createdAt;
}
