package com.petpick.petpick.controller.shop;

import java.util.List;
import java.util.Map;

import com.petpick.petpick.DTO.shop.CartItemRequest;
import com.petpick.petpick.DTO.shop.CartProductDTO;
import com.petpick.petpick.entity.shop.ShoppingCartItem;
import com.petpick.petpick.service.shop.ShoppingCartService;
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



@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /** 新增商品到購物車 */
    @PostMapping("/add")
    public ResponseEntity<ShoppingCartItem> addItemToCart(@RequestBody CartItemRequest request) {
        if (request == null || request.getUserId() == null || request.getProductId() == null) {
            return ResponseEntity.badRequest().build();
        }
        int qty = (request.getQuantity() == null || request.getQuantity() <= 0) ? 1 : request.getQuantity();
        ShoppingCartItem saved = shoppingCartService.addItemToCart(request.getUserId(), request.getProductId(), qty);
        return ResponseEntity.ok(saved);
    }

    /** 取得使用者的購物車內容（包含商品資訊） */
    @GetMapping("/withProduct/{userId}")
    public ResponseEntity<List<CartProductDTO>> getCartWithProductByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(shoppingCartService.getCartWithProductByUserId(userId));
    }

    /** 刪除購物車中的某個項目（以 cartId） */
    @DeleteMapping("/item/{cartId}")
    public ResponseEntity<Void> removeItemFromCart(@PathVariable Integer cartId) {
        shoppingCartService.removeItemFromCart(cartId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 清空指定使用者的購物車
     * - 新路徑：/clear/{userId}
     * - 舊路徑：/user/{userId}（為相容舊前端）
     */
    @DeleteMapping(path = { "/clear/{userId}", "/user/{userId}" })
    public ResponseEntity<Void> clearCart(@PathVariable Integer userId) {
        shoppingCartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    /** 更新購物車項目數量（以 cartId） */
    @PutMapping("/update")
    public ResponseEntity<ShoppingCartItem> updateQuantity(@RequestBody Map<String, Object> request) {
        if (request == null)
            return ResponseEntity.badRequest().build();

        Object cartIdObj = request.get("cartId");
        Object quantityObj = request.get("quantity");
        if (!(cartIdObj instanceof Number) || !(quantityObj instanceof Number)) {
            return ResponseEntity.badRequest().build();
        }
        Integer cartId = ((Number) cartIdObj).intValue();
        Integer quantity = Math.max(0, ((Number) quantityObj).intValue());

        ShoppingCartItem updated = shoppingCartService.updateQuantityByCartId(cartId, quantity);
        return ResponseEntity.ok(updated);
    }
}