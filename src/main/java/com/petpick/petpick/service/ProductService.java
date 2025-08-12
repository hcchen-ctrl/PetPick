package com.petpick.petpick.service;

import java.util.List;
import java.util.Optional;

import com.petpick.petpick.entity.Product;

public interface ProductService {

    List<Product> getAllProducts();

    Optional<Product> getProductById(Integer id);

    Product saveProduct(Product product);

    void deleteProduct(Integer id);

    List<Product> saveAllProducts(List<Product> products);
}


