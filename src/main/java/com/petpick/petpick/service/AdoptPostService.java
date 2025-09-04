package com.petpick.petpick.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.petpick.petpick.entity.AdoptPost;
import com.petpick.petpick.entity.PostReview;
import com.petpick.petpick.model.enums.PostStatus;
import com.petpick.petpick.repository.AdoptPostRepository;
import com.petpick.petpick.repository.PostReviewRepository;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

@Service
public class AdoptPostService {

    private final AdoptPostRepository postRepo;
    private final PostReviewRepository reviewRepo;

    public AdoptPostService(AdoptPostRepository postRepo, PostReviewRepository reviewRepo) {
        this.postRepo = postRepo;
        this.reviewRepo = reviewRepo;
    }

    // ✅ 新增：取得單一文章
    public AdoptPost getPostById(Long id) {
        return postRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在"));
    }

    // ✅ 新增：管理員查詢文章列表（支援分頁和篩選）
    public Page<AdoptPost> getPostsForAdmin(Integer page, Integer size, String status, String species, String q) {
        // 設定預設值
        if (page == null || page < 0) page = 0;
        if (size == null || size < 1) size = 24;

        // 建立分頁請求，按建立時間降序排列
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 建立動態查詢條件
        Specification<AdoptPost> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 狀態篩選
            if (status != null && !status.trim().isEmpty() && !"all".equals(status)) {
                try {
                    PostStatus postStatus = PostStatus.valueOf(status);
                    predicates.add(criteriaBuilder.equal(root.get("status"), postStatus));
                } catch (IllegalArgumentException e) {
                    // 忽略無效的狀態值
                }
            }

            // 物種篩選
            if (species != null && !species.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("species")),
                        "%" + species.toLowerCase() + "%"
                ));
            }

            // 關鍵字搜尋（搜尋標題、描述、品種）
            if (q != null && !q.trim().isEmpty()) {
                String keyword = "%" + q.toLowerCase() + "%";
                Predicate titleMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")), keyword);
                Predicate descMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")), keyword);
                Predicate breedMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("breed")), keyword);

                predicates.add(criteriaBuilder.or(titleMatch, descMatch, breedMatch));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return postRepo.findAll(spec, pageable);
    }

    @Transactional
    public void updateStatusAndLog(Long postId,
                                   PostStatus status,
                                   Long reviewerEmployeeId,
                                   String reason) {

        // ✅ 擴展允許的狀態，包含管理員操作
        if (status != PostStatus.pending &&
                status != PostStatus.approved &&
                status != PostStatus.rejected &&
                status != PostStatus.on_hold &&
                status != PostStatus.closed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "無效的狀態");
        }

        AdoptPost post = postRepo.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在"));

        if (post.getStatus() != status) {
            post.setStatus(status);
            postRepo.save(post);
        }

        // 記錄所有狀態變更
        PostReview r = new PostReview();
        r.setPostId(postId);

        // 根據狀態設定動作
        switch (status) {
            case approved -> r.setAction("approve");
            case rejected -> r.setAction("reject");
            case on_hold -> r.setAction("hold");
            case closed -> r.setAction("close");
            default -> r.setAction("update");
        }

        r.setReason(reason);
        r.setReviewerEmployeeId(reviewerEmployeeId == null ? null : reviewerEmployeeId.intValue());
        reviewRepo.save(r);
    }
}