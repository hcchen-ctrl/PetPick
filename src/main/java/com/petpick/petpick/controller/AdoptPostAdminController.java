package com.petpick.petpick.controller;

import com.petpick.petpick.model.enums.PostStatus;
import com.petpick.petpick.repository.PostReviewRepository;
import com.petpick.petpick.service.AdoptPostService;
import com.petpick.petpick.service.MyUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/posts") // 保持現有路徑，前端不需改
public class AdoptPostAdminController {

    private final AdoptPostService service;
    private final PostReviewRepository reviewRepo;

    public AdoptPostAdminController(AdoptPostService service, PostReviewRepository reviewRepo) {
        this.service = service;
        this.reviewRepo = reviewRepo;
    }

    @PatchMapping("/{id}/status")
    public void update(@PathVariable Long id,
                       @RequestParam PostStatus status,
                       @RequestParam(required = false) String reason) {
        requireAdmin(); // ✅ 改這裡
        Long reviewerId = getCurrentUserId(); // ✅ 改這裡
        service.updateStatusAndLog(id, status, reviewerId, reason);
    }

    @GetMapping("/{id}/reviews")
    public Object listReviews(@PathVariable Long id) {
        requireAdmin(); // ✅ 改這裡
        return reviewRepo.findByPostIdOrderByCreatedAtDesc(id);
    }

    // ✅ 改這裡：取得登入的 userId
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof MyUserDetails userDetails) {
            return userDetails.getId(); // 你自己的方法
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登入");
    }

    // ✅ 改這裡：檢查是否為 ADMIN
    private void requireAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof MyUserDetails userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登入");
        }
        if (!"ADMIN".equals(userDetails.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "僅管理員可操作");
        }
    }
}

