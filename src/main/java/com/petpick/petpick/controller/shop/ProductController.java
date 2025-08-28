// src/main/java/com/petpick/petpick/controller/ProductController.java
package com.petpick.petpick.controller.shop;

import java.net.URI;
import java.util.List;

import com.petpick.petpick.entity.shop.Product;
import com.petpick.petpick.service.shop.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@CrossOrigin // 如需跨網域可在此細調 allowedOrigins/allowedMethods
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    /**
     * 取得商品列表
     * 
     * @param published 可選：true 只回上架、false 只回下架、未帶回全部
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestParam(value = "published", required = false) Boolean published) {
        return ResponseEntity.ok(service.getAllProducts(published));
    }

    /** 取得單一商品 */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Integer id) {
        return service.getProductById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** 新增商品（預設可在 Entity 給 published=true） */
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product created = service.saveProduct(product);
        return ResponseEntity
                .created(URI.create("/api/products/" + created.getProductId()))
                .body(created);
    }

    /** 批次新增 */
    @PostMapping("/batch")
    public ResponseEntity<List<Product>> createProducts(@RequestBody List<Product> products) {
        return ResponseEntity.ok(service.saveAllProducts(products));
    }

    /** 全量更新商品 */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Integer id, @RequestBody Product product) {
        // 若資料不存在回 404
        return service.getProductById(id)
                .map(existing -> {
                    // 將路徑 id 覆寫進去（依你的欄位 setter 調整）
                    product.setProductId(id); // 若你的欄位是 getId()/setId() 請改用 setId(id)
                    Product saved = service.saveProduct(product);
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** 刪除商品 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer id) {
        if (service.getProductById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        service.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ===== 上/下架相關 =====

    /** 上架（published = true） */
    @PatchMapping("/{id}/publish")
    public ResponseEntity<Product> publish(@PathVariable Integer id) {
        return service.getProductById(id)
                .map(p -> ResponseEntity.ok(service.setPublished(id, true)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** 下架（published = false） */
    @PatchMapping("/{id}/unpublish")
    public ResponseEntity<Product> unpublish(@PathVariable Integer id) {
        return service.getProductById(id)
                .map(p -> ResponseEntity.ok(service.setPublished(id, false)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** 切換上/下架 */
    @PatchMapping("/{id}/toggle-publish")
    public ResponseEntity<Product> togglePublish(@PathVariable Integer id) {
        return service.getProductById(id)
                .map(p -> ResponseEntity.ok(service.togglePublished(id)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<Void> setActive(@PathVariable Integer id,
            @RequestParam boolean active) {
        var opt = service.getProductById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build(); // 404
        }
        var p = opt.get();
        p.setActive(active);
        service.saveProduct(p);
        return ResponseEntity.noContent().build(); // 204
    }
}