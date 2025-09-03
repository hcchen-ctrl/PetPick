package com.petpick.petpick.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.petpick.petpick.entity.AdoptPost;
import com.petpick.petpick.model.enums.PostStatus;


public interface AdoptPostRepository
        extends JpaRepository<AdoptPost, Long>,
        JpaSpecificationExecutor<AdoptPost> {

    // ① 讓「我的進度頁」撈到當前登入使用者的刊登（按建立時間新到舊）
    List<AdoptPost> findByPostedByUserIdOrderByCreatedAtDesc(Long postedByUserId);

    // 新增：依狀態過濾（審核中/已通過…）
    List<AdoptPost> findByPostedByUserIdAndStatusOrderByCreatedAtDesc(Long postedByUserId, PostStatus status);

// 在 AdoptPostRepository 介面中新增這些方法

    // 根據狀態查詢
    Page<AdoptPost> findByStatus(PostStatus status, Pageable pageable);

    // 根據物種查詢（忽略大小寫）
    Page<AdoptPost> findBySpeciesContainingIgnoreCase(String species, Pageable pageable);

    // 根據標題關鍵字查詢（忽略大小寫）
    Page<AdoptPost> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // 狀態 + 物種
    Page<AdoptPost> findByStatusAndSpeciesContainingIgnoreCase(PostStatus status, String species, Pageable pageable);

    // 狀態 + 標題
    Page<AdoptPost> findByStatusAndTitleContainingIgnoreCase(PostStatus status, String title, Pageable pageable);

    // 物種 + 標題
    Page<AdoptPost> findBySpeciesContainingIgnoreCaseAndTitleContainingIgnoreCase(String species, String title, Pageable pageable);

    // 三個條件都有
    Page<AdoptPost> findByStatusAndSpeciesContainingIgnoreCaseAndTitleContainingIgnoreCase(
            PostStatus status, String species, String title, Pageable pageable);
}