package com.petpick.petpick.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.petpick.petpick.dto.CartProductDTO;
import com.petpick.petpick.entity.Product;
import com.petpick.petpick.entity.ShoppingCartItem;
import com.petpick.petpick.repository.ProductRepository;
import com.petpick.petpick.repository.ShoppingCartRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShoppingCartService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    /**
     * 新增商品至購物車，如果該商品已存在則累加數量
     */
    public ShoppingCartItem addItemToCart(Integer userId, Integer productId, Integer quantity) {
        // 確認商品存在
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("找不到商品 ID: " + productId));

        // 查詢該用戶是否已有該商品
        Optional<ShoppingCartItem> existingItemOpt = shoppingCartRepository.findByUserIdAndProduct(userId, product);

        if (existingItemOpt.isPresent()) {
            // 更新數量和時間
            ShoppingCartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            existingItem.setAddedAt(LocalDateTime.now());
            return shoppingCartRepository.save(existingItem);
        } else {
            // 新增購物車項目
            ShoppingCartItem newItem = new ShoppingCartItem();
            newItem.setUserId(userId);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            // addedAt 會由 @PrePersist 自動填入
            return shoppingCartRepository.save(newItem);
        }
    }

    /**
     * 取得指定使用者的購物車項目（ShoppingCartItem 實體）
     */
    public List<ShoppingCartItem> getCartByUserId(Integer userId) {
        return shoppingCartRepository.findByUserId(userId);
    }

    public List<CartProductDTO> getCartWithProductByUserId(Integer userId) {
        return shoppingCartRepository.findCartItemsWithProductByUserId(userId).stream()
                .map(v -> new CartProductDTO(
                v.getCartId(),
                v.getQuantity(),
                v.getUserId(),
                v.getAddedAt(),
                v.getProductId(),
                v.getPname(),
                v.getImageUrl(),
                v.getPrice(), // BigDecimal
                v.getStock()
        ))
                .toList();
    }

    /**
     * 刪除購物車中的某商品
     */
    public void removeItemFromCart(Integer cartId) {
        if (!shoppingCartRepository.existsById(cartId)) {
            throw new RuntimeException("購物車中無此項目");
        }
        shoppingCartRepository.deleteById(cartId);
    }

    /**
     * 刪除購物車中的全部商品
     */
    @Transactional
    public int clearCart(Integer userId) {
        int affected = shoppingCartRepository.deleteByUserId(userId);
        System.out.println("[Cart] clear userId=" + userId + ", affected=" + affected);
        return affected;
    }

    /**
     * 更新購物車中特定商品數量
     */
    public ShoppingCartItem updateQuantityByCartId(Integer cartId, Integer quantity) {
        ShoppingCartItem item = shoppingCartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("購物車中無此商品"));

        item.setQuantity(quantity);
        return shoppingCartRepository.save(item);
    }
}
