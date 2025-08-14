package com.petpick.petpick.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick.dto.CartItemRequest;
import com.petpick.petpick.dto.CartProductDTO;
import com.petpick.petpick.entity.ShoppingCartItem;
import com.petpick.petpick.service.ShoppingCartService;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins="*")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 新增商品到購物車
     */
    @PostMapping("/add")
    public ShoppingCartItem addItemToCart(@RequestBody CartItemRequest request) {
        return shoppingCartService.addItemToCart(request.getUserId(), request.getProductId(), request.getQuantity());
    }

    /**
     * 取得使用者的購物車內容
     */
    @GetMapping("/withProduct/{userId}")
    public List<CartProductDTO> getCartWithProductByUserId(@PathVariable Integer userId) {
        return shoppingCartService.getCartWithProductByUserId(userId);
    }

    /**
     * 刪除購物車中的某個商品
     */
    @DeleteMapping("/item/{cartId}")
    public ResponseEntity<Void> removeItemFromCart(@PathVariable Integer cartId) {
        shoppingCartService.removeItemFromCart(cartId);
        return ResponseEntity.noContent().build(); // 204
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> clearCart(@PathVariable Integer userId) {
        shoppingCartService.clearCart(userId);
        return ResponseEntity.noContent().build(); // 204
    }

    /**
     * 更新購物車商品數量
     */
    @PutMapping("/update")
    public ShoppingCartItem updateQuantity(@RequestBody Map<String, Object> request) {
        Integer cartId = (Integer) request.get("cartId");
        Integer quantity = (Integer) request.get("quantity");
        return shoppingCartService.updateQuantityByCartId(cartId, quantity);
    }
}
