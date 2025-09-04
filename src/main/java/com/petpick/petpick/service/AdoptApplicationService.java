package com.petpick.petpick.service;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus; // 修正為 UserEntity
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.petpick.petpick.DTO.ApplicationDTO;
import com.petpick.petpick.entity.AdoptApplication;
import com.petpick.petpick.entity.AdoptPost; // 修正為 UserRepository
import com.petpick.petpick.model.enums.ApplicationStatus;
import com.petpick.petpick.model.enums.PostStatus;
import com.petpick.petpick.model.enums.SourceType;
import com.petpick.petpick.repository.AdoptApplicationRepository;
import com.petpick.petpick.repository.AdoptPostRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class AdoptApplicationService {

    private final AdoptApplicationRepository appRepo;
    private final AdoptPostRepository postRepo;

    // 送出申請（官方貼文才開放）
    @Transactional
    public ApplicationDTO apply(Long postId, Long uid, String message){
        AdoptPost post = postRepo.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "貼文不存在"));

        if (post.getSourceType() != SourceType.platform)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "這是民眾貼文，直接聯絡即可");

        // 只允許在「上架中」時申請（on_hold/closed 都不行）
        if (post.getStatus() != PostStatus.approved)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "貼文未上架，無法申請");

        // 若此貼文已核准他人，直接擋下
        if (appRepo.existsByPostIdAndStatus(postId, ApplicationStatus.approved))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "此貼文已完成配對");

        try {
            // 先找是否已有舊紀錄
            var existed = appRepo.findTopByPostIdAndApplicantUserIdOrderByIdDesc(postId, uid).orElse(null);
            if (existed != null) {
                // 已申請中/已通過 → 禁止重送
                if (existed.getStatus() == ApplicationStatus.pending || existed.getStatus() == ApplicationStatus.approved) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "你已申請過了");
                }
                // 已退回/已取消 → 直接重開同一筆
                existed.setStatus(ApplicationStatus.pending);
                existed.setMessage(message);
                existed.setReviewedByEmployeeId(null);
                existed.setApprovedAt(null);
                existed.setRejectedAt(null);
                existed.setRejectReason(null);
                existed.setUpdatedAt(LocalDateTime.now());
                var saved = appRepo.save(existed);
                return ApplicationDTO.from(saved, post);
            }

            // 第一次申請 → 新增
            AdoptApplication a = new AdoptApplication();
            a.setPostId(postId);
            a.setApplicantUserId(uid);
            a.setMessage(message);
            a.setStatus(ApplicationStatus.pending);
            var saved = appRepo.save(a);
            return ApplicationDTO.from(saved, post);

        } catch (DataIntegrityViolationException e) {
            // 少數併發情況下同人連點造成唯一鍵衝突 → 統一回 409
            throw new ResponseStatusException(HttpStatus.CONFLICT, "你已申請過了");
        }
    }


    // 我的申請列表
    public Page<ApplicationDTO> myApps(Long uid, String status, Pageable pageable){
        Page<AdoptApplication> page;
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            page = appRepo.findByApplicantUserId(uid, pageable);
        } else {
            ApplicationStatus st = ApplicationStatus.valueOf(status.toLowerCase());
            page = appRepo.findByApplicantUserIdAndStatus(uid, st, pageable);
        }
        // 複合成 DTO（載入貼文摘要）
        return page.map(a -> {
            AdoptPost p = postRepo.findById(a.getPostId()).orElse(null);
            return ApplicationDTO.from(a, p);
        });
    }

    // 取消（只有 pending 才能取消）
    public void cancel(Long appId, Long uid){
        AdoptApplication a = appRepo.findById(appId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!Objects.equals(a.getApplicantUserId(), uid))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (a.getStatus() != ApplicationStatus.pending)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有審核中才能取消");
        a.setStatus(ApplicationStatus.cancelled);
        a.setUpdatedAt(LocalDateTime.now());
        appRepo.save(a);
    }

    // ===== 管理員 =====

    public Page<ApplicationDTO> adminSearch(String status, String species, String q, Pageable pageable){
        ApplicationStatus st = null;
        if (status != null && !"all".equalsIgnoreCase(status)) {
            st = ApplicationStatus.valueOf(status.toLowerCase());
        }
        var page = appRepo.adminSearch(st, species, q, pageable);
        return page.map(a -> {
            AdoptPost p = postRepo.findById(a.getPostId()).orElse(null);
            return ApplicationDTO.from(a, p);
        });
    }

    public ApplicationDTO get(Long id){
        AdoptApplication a = appRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        AdoptPost p = postRepo.findById(a.getPostId()).orElse(null);
        return ApplicationDTO.from(a, p);
    }

    @Transactional
    public void approve(Long id, Long reviewerId){
        AdoptApplication a = appRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (a.getStatus() != ApplicationStatus.pending)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只能核准審核中申請");

        Long postId = a.getPostId();

        // 若已有人被核准，阻止重複核准
        if (appRepo.existsByPostIdAndStatus(postId, ApplicationStatus.approved)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "此貼文已核准他人");
        }

        // 1) 核准這一筆
        a.setStatus(ApplicationStatus.approved);
        a.setReviewedByEmployeeId(reviewerId);
        a.setApprovedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        appRepo.save(a);

        // 2) 其它 pending 全部退回
        appRepo.rejectOthersPendingOfPost(
                postId,
                a.getId(),
                ApplicationStatus.pending,
                ApplicationStatus.rejected,
                "已由其他申請者獲准"
        );


        // 3) 關閉貼文，不再接受新申請
        AdoptPost post = postRepo.findById(postId).orElse(null);
        if (post != null && post.getStatus() != PostStatus.closed) {
            post.setStatus(PostStatus.closed);
            postRepo.save(post);
        }
    }


    public void reject(Long id, Long reviewerId, String reason){
        AdoptApplication a = appRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (a.getStatus() != ApplicationStatus.pending)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        a.setStatus(ApplicationStatus.rejected);
        a.setReviewedByEmployeeId(reviewerId);
        a.setRejectedAt(LocalDateTime.now());
        a.setRejectReason(reason);
        a.setUpdatedAt(LocalDateTime.now());
        appRepo.save(a);
    }
}