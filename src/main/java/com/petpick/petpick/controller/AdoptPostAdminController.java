package com.petpick.petpick.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.petpick.petpick.repository.PostReviewRepository;
import com.petpick.petpick.service.AdoptPostService;

import jakarta.servlet.http.HttpSession;


@RestController
@RequestMapping("/api/posts") // 保持現有路徑，前端不需改
public class AdoptPostAdminController {

    private final AdoptPostService service;
    private final PostReviewRepository reviewRepo;

    public AdoptPostAdminController(AdoptPostService service, PostReviewRepository reviewRepo) {
        this.service = service;
        this.reviewRepo = reviewRepo;
    }



    @GetMapping("/{id}/reviews")
    public Object listReviews(@PathVariable Long id, HttpSession session) {
        requireAdmin(session);
        return reviewRepo.findByPostIdOrderByCreatedAtDesc(id);
    }

    private void requireAdmin(HttpSession s) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "僅管理員可操作");
        }
    }
}