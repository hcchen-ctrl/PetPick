package com.petpick.petpick.controller;

import com.petpick.petpick.DTO.User.BatchAccountStatusRequest;
import com.petpick.petpick.DTO.User.BatchBlacklistStatusRequest;
import com.petpick.petpick.DTO.User.PasswordResetRequest;
import com.petpick.petpick.entity.UserEntity;
import com.petpick.petpick.service.UserService;
import com.petpick.petpick.service.UserServiceV2;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// UserController.java
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserServiceV2 userServiceV2;

    // 分頁查詢會員列表
    @GetMapping
    public ResponseEntity<Page<UserEntity>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String isaccount,
            @RequestParam(required = false) String isblacklist,
            @RequestParam(required = false) String role) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<UserEntity> users = userServiceV2.searchUsers(q, isaccount, isblacklist, role, pageable);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 更新會員資料
    @PutMapping("/{userid}")
    public ResponseEntity<String> updateUser(@PathVariable Long userid, @RequestBody UserEntity updateData) {
        try {
            userServiceV2.updateUser(userid, updateData);
            return ResponseEntity.ok("更新成功");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("更新失敗: " + e.getMessage());
        }
    }

    // 重設密碼
    @PutMapping("/{userid}/password")
    public ResponseEntity<String> resetPassword(@PathVariable Long userid, @RequestBody PasswordResetRequest request) {
        try {
            userServiceV2.resetPassword(userid, request.getPassword());
            return ResponseEntity.ok("密碼重設成功");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("密碼重設失敗: " + e.getMessage());
        }
    }

    // 批次更新帳戶狀態
    @PostMapping("/batch-update-account-status")
    public ResponseEntity<String> batchUpdateAccountStatus(@RequestBody BatchAccountStatusRequest request) {
        try {
            userServiceV2.batchUpdateAccountStatus(request.getUserids(), request.getIsaccount(), request.getReason());
            return ResponseEntity.ok("批次更新成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("批次更新失敗: " + e.getMessage());
        }
    }

    // 批次更新黑名單狀態
    @PostMapping("/batch-update-blacklist-status")
    public ResponseEntity<String> batchUpdateBlacklistStatus(@RequestBody BatchBlacklistStatusRequest request) {
        try {
            userServiceV2.batchUpdateBlacklistStatus(request.getUserids(), request.getIsblacklist(), request.getReason());
            return ResponseEntity.ok("批次更新成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("批次更新失敗: " + e.getMessage());
        }
    }

    // 刪除會員
    @DeleteMapping("/{userid}")
    public ResponseEntity<String> deleteUser(@PathVariable Long userid, @RequestParam String reason) {
        try {
            userServiceV2.deleteUser(userid, reason);
            return ResponseEntity.ok("刪除成功");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("刪除失敗: " + e.getMessage());
        }
    }
}
