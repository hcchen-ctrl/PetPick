// src/main/java/com/petpick/petpick/service/impl/ProductServiceImpl.java
package com.petpick.petpick.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petpick.petpick.entity.Product;
import com.petpick.petpick.repository.ProductRepository;
import com.petpick.petpick.service.ProductService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repo;

    @Override
    public List<Product> getAllProducts() {
        return repo.findAll();
    }

    @Override
    public List<Product> getAllProducts(Boolean published) {
        if (published == null) return repo.findAll();
        return published ? repo.findAllByPublishedTrue() : repo.findAllByPublishedFalse();
    }

    @Override
    public Optional<Product> getProductById(Integer id) {
        return repo.findById(id);
    }

    @Override
    @Transactional
    public Product saveProduct(Product product) {
        return repo.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Integer id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public List<Product> saveAllProducts(List<Product> products) {
        return repo.saveAll(products);
    }

    @Override
    @Transactional
    public Product setPublished(Integer id, boolean published) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        p.setPublished(published);
        return repo.save(p);
    }

    @Override
    @Transactional
    public Product togglePublished(Integer id) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        p.setPublished(!p.isPublished());
        return repo.save(p);
    }
}