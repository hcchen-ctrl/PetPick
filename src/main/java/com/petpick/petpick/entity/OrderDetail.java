// OrderDetail.java
package com.petpick.petpick.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Data
// OrderDetail.java
@Entity
@Table(name = "order_details")
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // ★ 整數單價
    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    // ★ 整數小計
    @Column(name = "subtotal", nullable = false)
    private Integer subtotal;

    @PrePersist
    @PreUpdate
    private void calcSubtotal() {
        int up = unitPrice == null ? 0 : unitPrice;
        int q  = quantity  == null ? 0 : quantity;
        long s = (long) up * (long) q; // 先用 long 相乘避免溢位
        if (s > Integer.MAX_VALUE) s = Integer.MAX_VALUE;
        if (s < Integer.MIN_VALUE) s = Integer.MIN_VALUE;
        this.subtotal = (int) s;
    }

    /** 若你在 Service 端手動呼叫重算 */
    public void recomputeSubtotal() { calcSubtotal(); }

    // getters/setters ...
}