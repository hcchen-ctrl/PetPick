package com.petpick.petpick.DTO;

import java.time.LocalDateTime;

import com.petpick.petpick.entity.AdoptApplication;
import com.petpick.petpick.entity.AdoptPost;
import lombok.Data;

@Data
public class ApplicationDTO {
    private Long id;
    private Long postId;
    private Long applicantUserId;
    private String applicantName; // 👈 新增欄位
    private String message;
    private String status;
    private Long reviewedByEmployeeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private String rejectReason;

    private PostSummaryDTO post; // 讓前端可顯示貼文縮圖/標題

    public static ApplicationDTO from(AdoptApplication a, AdoptPost p){
        ApplicationDTO d = new ApplicationDTO();
        d.id = a.getId();
        d.postId = a.getPostId();
        d.applicantUserId = a.getApplicantUserId();
        d.applicantName = (a.getApplicant() != null) ? a.getApplicant().getUsername() : "undefined"; // 👈 安全取值
        d.message = (a.getMessage() != null) ? a.getMessage() : ""; // 👈 避免 null 顯示在前端
        d.status = a.getStatus().name();
        d.reviewedByEmployeeId = a.getReviewedByEmployeeId();
        d.createdAt = a.getCreatedAt();
        d.updatedAt = a.getUpdatedAt();
        d.approvedAt = a.getApprovedAt();
        d.rejectedAt = a.getRejectedAt();
        d.rejectReason = a.getRejectReason();
        if (p != null) d.post = PostSummaryDTO.from(p);
        return d;
    }
}
