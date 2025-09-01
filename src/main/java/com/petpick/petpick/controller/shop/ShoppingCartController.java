package com.petpick.petpick.controller.shop;

import java.util.List;
import java.util.Map;

import com.petpick.petpick.DTO.shop.CartItemRequest;
import com.petpick.petpick.DTO.shop.CartProductDTO;
import com.petpick.petpick.entity.shop.ShoppingCartItem;
import com.petpick.petpick.service.shop.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
// ✅ 移除這個註解，讓 SecurityConfig 處理 CORS
// @CrossOrigin(origins = "*")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /** 新增商品到購物車 */
    @PostMapping("/add")
    public ResponseEntity<ShoppingCartItem> addItemToCart(@RequestBody CartItemRequest request) {
        System.out.println("🛒 收到加入購物車請求: " + request);

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
        System.out.println("🛒 收到取得購物車請求: userId=" + userId);

        List<CartProductDTO> result = shoppingCartService.getCartWithProductByUserId(userId);
        if (result == null) {
            result = List.of();
        }
        return ResponseEntity.ok(result);
    }

    /** 刪除購物車中的某個項目（以 cartId） */
    @DeleteMapping("/item/{cartId}")
    public ResponseEntity<Void> removeItemFromCart(@PathVariable Integer cartId) {
        System.out.println("🗑️ 收到移除商品請求: cartId=" + cartId);

        shoppingCartService.removeItemFromCart(cartId);
        return ResponseEntity.noContent().build();
    }

    /** 清空指定使用者的購物車 */
    @DeleteMapping(path = { "/clear/{userId}", "/user/{userId}" })
    public ResponseEntity<Void> clearCart(@PathVariable Integer userId) {
        System.out.println("🗑️ 收到清空購物車請求: userId=" + userId);

        shoppingCartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    /** 更新購物車項目數量（以 cartId） */
    @PutMapping("/update")
    public ResponseEntity<ShoppingCartItem> updateQuantity(@RequestBody Map<String, Object> request) {
        System.out.println("🔄 收到更新數量請求: " + request);

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