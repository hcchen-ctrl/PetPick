package tw.petpick.petpick.dto;

import java.time.LocalDateTime;

import lombok.Data;
import tw.petpick.petpick.model.AdoptApplication;
import tw.petpick.petpick.model.AdoptPost;

@Data
public class ApplicationDTO {
    private Long id;
    private Long postId;
    private Long applicantUserId;
    private String applicantName;        // ✅ 新增
    private String message;
    private String status;
    private Long reviewedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private String rejectReason;

    private PostSummaryDTO post;

    // ✅ 原本版本（不帶姓名，避免舊程式炸掉）
    public static ApplicationDTO from(AdoptApplication a, AdoptPost p){
        return from(a, p, null);
    }

    // ✅ 新增版本：可帶申請者名字
    public static ApplicationDTO from(AdoptApplication a, AdoptPost p, String applicantName){
        ApplicationDTO d = new ApplicationDTO();
        d.id = a.getId();
        d.postId = a.getPostId();
        d.applicantUserId = a.getApplicantUserId();
        d.applicantName = applicantName;                // ← 這裡才有值
        d.message = a.getMessage();
        d.status = a.getStatus().name();
        d.reviewedByUserId = a.getReviewedByUserId();
        d.createdAt = a.getCreatedAt();
        d.updatedAt = a.getUpdatedAt();
        d.approvedAt = a.getApprovedAt();
        d.rejectedAt = a.getRejectedAt();
        d.rejectReason = a.getRejectReason();
        if (p != null) d.post = PostSummaryDTO.from(p);
        return d;
    }
}

