// src/main/java/com/petpick/petpick/service/ProductService.java
package com.petpick.petpick.service;

import java.util.List;
import java.util.Optional;

import com.petpick.petpick.entity.Product;

public interface ProductService {

    /** 取得全部商品（不分上/下架） */
    List<Product> getAllProducts();

    /** 依上架狀態過濾。published==true 只回上架；false 只回下架；null 全部 */
    List<Product> getAllProducts(Boolean published);

    Optional<Product> getProductById(Integer id);

    Product saveProduct(Product product);

    void deleteProduct(Integer id);

    List<Product> saveAllProducts(List<Product> products);

    /** 設定上/下架狀態 */
    Product setPublished(Integer id, boolean published);

    /** 切換上/下架狀態（上架→下架、下架→上架） */
    Product togglePublished(Integer id);
}