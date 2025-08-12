package com.petpick.petpick.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "pname", nullable = false, length = 100)
    private String pname;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "type")
    private String type;

    // --- Constructors ---
    public Product() {
    }

    public Product(Integer productId, String pname, String description, Integer price, Integer stock,
            Integer categoryId, String imageUrl, LocalDateTime createdAt, Boolean isActive, String type) {
        this.productId = productId;
        this.pname = pname;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.categoryId = categoryId;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.isActive = isActive;
        this.type = type;
    }

    // 一個商品可以有多個購物車項目
    @OneToMany(mappedBy = "product")
    @JsonManagedReference
    private List<ShoppingCartItem> shoppingCartItems;
}
