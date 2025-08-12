package com.petpick.petpick.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "orders")
public class Order {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer orderId;
  private Integer userId;
  private Integer employeeId; // 可為 null
  private Integer totalPrice;
  private String status;      // 建議用 enum 映射字串
  private LocalDateTime createdAt;
  private LocalDateTime shippedAt;
  private String addr;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
  private List<OrderDetail> items = new ArrayList<>();
  // getters/setters...
}
